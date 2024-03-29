package us.huseli.thoucylinder.viewmodels

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.DpSize
import androidx.media3.common.PlaybackException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.huseli.retaintheme.extensions.combineEquals
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.enums.RadioType
import us.huseli.thoucylinder.dataclasses.BaseArtist
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.abstr.toArtists
import us.huseli.thoucylinder.dataclasses.callbacks.RadioCallbacks
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.PlaylistTrack
import us.huseli.thoucylinder.dataclasses.entities.Radio
import us.huseli.thoucylinder.dataclasses.entities.Tag
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistPojo
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyTrack
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyTrackRecommendations
import us.huseli.thoucylinder.dataclasses.views.AlbumCombo
import us.huseli.thoucylinder.dataclasses.views.QueueTrackCombo
import us.huseli.thoucylinder.dataclasses.views.RadioCombo
import us.huseli.thoucylinder.dataclasses.views.toTrackArtists
import us.huseli.thoucylinder.interfaces.PlayerRepositoryListener
import us.huseli.thoucylinder.launchOnIOThread
import us.huseli.thoucylinder.repositories.PlayerRepository
import us.huseli.thoucylinder.repositories.Repositories
import us.huseli.thoucylinder.umlautify
import java.util.UUID
import javax.inject.Inject
import kotlin.math.min
import kotlin.random.Random

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repos: Repositories,
    @ApplicationContext context: Context,
) : DownloadsViewModel(repos), PlayerRepositoryListener {
    private var deletedPlaylist: Playlist? = null
    private var deletedPlaylistTracks: List<PlaylistTrack> = emptyList()
    private var enqueueRadioTracksJob: Job? = null
    private var radioHasMoreTracks = true
    private val radioUsedLocalTrackIds = mutableListOf<UUID>()
    private val radioUsedSpotifyTrackIds = mutableListOf<String>()

    val activeRadio: StateFlow<RadioCombo?> = repos.radio.activeRadio
    val isWelcomeDialogShown: StateFlow<Boolean> = repos.settings.isWelcomeDialogShown
    val libraryRadioNovelty: StateFlow<Float> = repos.settings.libraryRadioNovelty
    val playlists: Flow<List<PlaylistPojo>> = repos.playlist.playlistsPojos
    val umlautify: StateFlow<Boolean> = repos.settings.umlautify

    init {
        repos.player.addListener(this)
        repos.musicBrainz.startMatchingArtists(repos.artist.artistsWithTracksOrAlbums) { artistId, musicBrainzId ->
            repos.artist.setArtistMusicBrainzId(artistId, musicBrainzId)
        }
        repos.spotify.startMatchingArtists(repos.artist.artistsWithTracksOrAlbums) { artistId, spotifyId, image ->
            repos.artist.setArtistSpotifyData(artistId, spotifyId, image)
        }

        launchOnIOThread {
            repos.radio.activeRadio.filterNotNull().collect {
                if (it.isInitialized) restartRadio(it)
                else initializeRadio(it, context)
            }
        }
    }

    fun addAlbumsToLibrary(albumIds: Collection<UUID>) = launchOnIOThread {
        repos.album.addAlbumsToLibrary(albumIds)
        repos.track.addToLibraryByAlbumId(albumIds)
    }

    fun addSelectionToPlaylist(
        selection: Selection,
        playlistId: UUID,
        includeDuplicates: Boolean = true,
        onFinish: (added: Int) -> Unit = {},
    ) = launchOnIOThread {
        onFinish(repos.playlist.addSelectionToPlaylist(selection, playlistId, includeDuplicates))
    }

    fun createPlaylist(playlist: Playlist, selection: Selection? = null) = launchOnIOThread {
        repos.playlist.insertPlaylist(playlist)
        selection?.also { repos.playlist.addSelectionToPlaylist(it, playlist.playlistId) }
    }

    fun deletePlaylist(playlist: Playlist, onFinish: () -> Unit = {}) = launchOnIOThread {
        deletedPlaylist = playlist
        deletedPlaylistTracks = repos.playlist.listPlaylistTracks(playlist.playlistId)
        repos.playlist.deletePlaylist(playlist)
        onFinish()
    }

    fun deleteLocalAlbumFiles(albumIds: Collection<UUID>, onFinish: () -> Unit = {}) = launchOnIOThread {
        repos.album.setAlbumsIsLocal(albumIds, false)
        repos.album.listAlbumsWithTracks(albumIds).forEach { combo ->
            deleteLocalAlbumFiles(combo)
        }
        onFinish()
    }

    fun doStartupTasks(context: Context) = launchOnIOThread {
        updateGenreList()
        if (repos.settings.autoImportLocalMusic.value == true) importNewLocalAlbums(context)
        findOrphansAndDuplicates()
        repos.playlist.deleteOrphanPlaylistTracks()
        repos.track.deleteTempTracks()
        repos.album.deleteTempAlbums()
        deleteMarkedAlbums()
        // repos.spotify.fetchTrackAudioFeatures(repos.track.listTrackSpotifyIds())
    }

    suspend fun getDuplicatePlaylistTrackCount(playlistId: UUID, selection: Selection) = withContext(Dispatchers.IO) {
        repos.playlist.getDuplicatePlaylistTrackCount(playlistId, selection)
    }

    suspend fun getAlbumCombo(albumId: UUID): AlbumCombo? =
        withContext(Dispatchers.IO) { repos.album.getAlbumCombo(albumId) }

    fun hideAlbums(albumIds: Collection<UUID>, onFinish: () -> Unit = {}) = launchOnIOThread {
        repos.album.setAlbumsIsHidden(albumIds, true)
        onFinish()
    }

    fun hideAlbumsAndDeleteFiles(albumIds: Collection<UUID>, onFinish: () -> Unit = {}) = launchOnIOThread {
        repos.album.setAlbumsIsHidden(albumIds, true)
        repos.album.listAlbumsWithTracks(albumIds).forEach { combo ->
            deleteLocalAlbumFiles(combo)
        }
        onFinish()
    }

    suspend fun listSelectionTracks(selection: Selection) =
        withContext(Dispatchers.IO) { repos.playlist.listSelectionTracks(selection) }

    fun removeAlbumsFromLibrary(albumIds: Collection<UUID>, onFinish: () -> Unit = {}) = launchOnIOThread {
        repos.album.removeAlbumsFromLibrary(albumIds)
        repos.track.removeFromLibraryByAlbumId(albumIds)
        onFinish()
    }

    fun setInnerPadding(value: PaddingValues) = repos.settings.setInnerPadding(value)

    fun setLibraryRadioNovelty(value: Float) = repos.settings.setLibraryRadioNovelty(value)

    fun setLocalMusicUri(value: Uri) = repos.settings.setLocalMusicUri(value)

    fun setContentAreaSize(value: DpSize) = repos.settings.setContentAreaSize(value)

    fun setWelcomeDialogShown(value: Boolean) = repos.settings.setWelcomeDialogShown(value)

    fun startAlbumRadio(albumId: UUID) = launchOnIOThread {
        repos.radio.setActiveRadio(Radio(albumId = albumId, type = RadioType.ALBUM))
    }

    fun startArtistRadio(artistId: UUID) = launchOnIOThread {
        repos.radio.setActiveRadio(Radio(artistId = artistId, type = RadioType.ARTIST))
    }

    fun startLibraryRadio() = launchOnIOThread {
        repos.radio.setActiveRadio(Radio(type = RadioType.LIBRARY))
    }

    fun startTrackRadio(trackId: UUID) = launchOnIOThread {
        repos.radio.setActiveRadio(Radio(trackId = trackId, type = RadioType.TRACK))
    }

    fun undoDeletePlaylist(onFinish: (UUID) -> Unit) = launchOnIOThread {
        deletedPlaylist?.also { playlist ->
            repos.playlist.insertPlaylist(playlist)
            repos.playlist.insertPlaylistTracks(deletedPlaylistTracks)
            deletedPlaylist = null
            deletedPlaylistTracks = emptyList()
            onFinish(playlist.playlistId)
        }
    }

    fun unhideAlbums(albumIds: Collection<UUID>) = launchOnIOThread {
        repos.album.setAlbumsIsHidden(albumIds, false)
    }


    /** PRIVATE METHODS ******************************************************/
    private suspend fun deleteLocalAlbumFiles(combo: AlbumWithTracksCombo) {
        repos.localMedia.deleteAlbumDirectoryAlbumArt(
            albumCombo = combo,
            albumDirectory = repos.settings.getAlbumDirectory(combo),
            tracks = combo.trackCombos.map { it.track },
        )
        repos.track.deleteTrackFiles(combo.trackCombos.map { it.track })
        repos.track.clearLocalUris(combo.trackCombos.map { it.track.trackId })
    }

    private suspend fun deleteMarkedAlbums() {
        val combos = repos.album.listDeletionMarkedAlbumCombos()

        combos.forEach {
            it.album.albumArt?.deleteInternalFiles()
            repos.localMedia.deleteAlbumDirectoryAlbumArt(
                albumCombo = it,
                albumDirectory = repos.settings.getAlbumDirectory(it),
            )
        }
        if (combos.isNotEmpty()) {
            repos.track.deleteTracksByAlbumId(combos.map { it.album.albumId })
            repos.album.deleteAlbums(combos.map { it.album })
        }
    }

    private suspend fun enqueueLibraryRadioTracks(
        recommendations: SpotifyTrackRecommendations,
        channel: Channel<QueueTrackCombo?>,
        limit: Int,
    ) {
        /**
         * For every iteration, there is a probability of `libraryRadioNovelty` that the next track to add should be a
         * recommendation track (rather than a library track). This works because `libraryRadioNovelty` is a float
         * ranging between 0f and 1f, where 1f represents 100% recommendation tracks.
         */
        var recommendationTrackIdx = 0

        for (i in 0 until limit) {
            val nextIsLibraryTrack = Random.nextFloat() >= libraryRadioNovelty.value
            var queueTrackCombo: QueueTrackCombo? = null

            if (nextIsLibraryTrack) {
                queueTrackCombo = getRandomLibraryQueueTrackCombo(
                    exceptSpotifyTrackIds = radioUsedSpotifyTrackIds,
                    exceptTrackIds = radioUsedLocalTrackIds,
                )?.also { radioUsedLocalTrackIds.add(it.track.trackId) }
            } else {
                while (queueTrackCombo == null && recommendationTrackIdx < recommendations.tracks.size) {
                    queueTrackCombo = spotifyTrackToQueueTrackCombo(
                        spotifyTrack = recommendations.tracks[recommendationTrackIdx++]
                    )
                }
            }

            if (queueTrackCombo != null) channel.trySend(queueTrackCombo)
            else break
        }
    }

    private suspend fun enqueueRadioTracks(
        radioId: UUID,
        radioType: RadioType,
        recommendations: SpotifyTrackRecommendations,
        channel: Channel<QueueTrackCombo?>,
        limit: Int,
    ) {
        if (!recommendations.hasMore) radioHasMoreTracks = false

        if (radioType == RadioType.LIBRARY) {
            enqueueLibraryRadioTracks(
                recommendations = recommendations,
                channel = channel,
                limit = limit,
            )
        } else {
            var addedTracks = 0

            for (spotifyTrack in recommendations.tracks) {
                spotifyTrackToQueueTrackCombo(spotifyTrack)?.also {
                    channel.trySend(it)
                    addedTracks++
                }
                if (addedTracks >= limit) break
            }
        }

        // Null signals to PlayerRepository that this batch is finished.
        channel.trySend(null)
        repos.radio.updateRadio(
            radioId = radioId,
            spotifyTrackIds = radioUsedSpotifyTrackIds,
            localTrackIds = radioUsedLocalTrackIds,
        )
    }

    private suspend fun findOrphansAndDuplicates() {
        val allTracks = repos.track.listTracks()
        val allAlbums = repos.album.listAlbums()
        val allAlbumMultimap =
            allAlbums.associateWith { album -> allTracks.filter { it.albumId == album.albumId } }
        val nonAlbumDuplicateTracks = allTracks
            .combineEquals { a, b -> a.localUri == b.localUri && a.youtubeVideo?.id == b.youtubeVideo?.id }
            .filter { tracks -> tracks.size > 1 }
            .map { tracks -> tracks.filter { it.albumId == null } }
            .flatten()
        // Collect tracks with non-working localUris:
        val brokenUriTracks = repos.localMedia.listTracksWithBrokenLocalUris(allTracks)
        val nonLocalTracks = brokenUriTracks + allTracks.filter { it.localUri == null }
        // Collect albums that have isLocal=true but should have false:
        val noLongerLocalAlbums = allAlbumMultimap
            .filterKeys { it.isLocal }
            .filterValues { nonLocalTracks.containsAll(it) }

        // Delete non-album tracks that have duplicates on albums:
        repos.track.deleteTracks(nonAlbumDuplicateTracks)
        // Update tracks with broken localUris:
        repos.track.clearLocalUris(brokenUriTracks.map { it.trackId })
        // Update albums that should have isLocal=true, but don't:
        repos.album.setAlbumsIsLocal(noLongerLocalAlbums.keys.map { it.albumId }, false)
    }

    private suspend fun getInitialRadioRecommendations(radio: RadioCombo): SpotifyTrackRecommendations? =
        when (radio.type) {
            RadioType.LIBRARY -> repos.spotify.getTrackRecommendations(
                spotifyTrackIds = listRandomLibrarySpotifyTrackIds(5),
                limit = 40,
            )
            RadioType.ARTIST -> radio.artist?.let { artist ->
                repos.spotify.getTrackRecommendationsByArtist(artist, 40)
            }
            RadioType.ALBUM -> radio.album?.let { album ->
                repos.album.getAlbumWithTracks(album.albumId)
                    ?.let { repos.spotify.getTrackRecommendationsByAlbumCombo(it, 40) }
            }
            RadioType.TRACK -> radio.track?.let { track ->
                val albumCombo = track.albumId?.let { repos.album.getAlbumCombo(it) }
                val artists = repos.artist.listTrackArtistCredits(track.trackId).toArtists()
                    .plus(albumCombo?.artists?.toArtists() ?: emptyList())

                repos.spotify.getTrackRecommendationsByTrack(
                    track = track,
                    album = albumCombo?.album,
                    artists = artists,
                    limit = 40,
                )
            }
        }?.also { radioUsedSpotifyTrackIds.addAll(it.tracks.map { track -> track.id }) }

    private fun getRadioCallbacks(
        radioId: UUID,
        radioType: RadioType,
        channel: Channel<QueueTrackCombo?>,
    ) = RadioCallbacks(
        deactivate = {
            repos.radio.deactivateRadio()
            channel.close()
            enqueueRadioTracksJob?.cancel()
            enqueueRadioTracksJob = null
        },
        requestMoreTracks = {
            if (radioHasMoreTracks) enqueueRadioTracksJob = repos.globalScope.launch {
                val recommendations =
                    repos.spotify.getTrackRecommendations(spotifyTrackIds = radioUsedSpotifyTrackIds, limit = 20)
                        .also { radioUsedSpotifyTrackIds.addAll(it.tracks.map { track -> track.id }) }

                enqueueRadioTracks(
                    radioId = radioId,
                    radioType = radioType,
                    recommendations = recommendations,
                    channel = channel,
                    limit = 10,
                )
            }
        },
    )

    private suspend fun getRandomLibraryQueueTrackCombo(
        exceptTrackIds: Collection<UUID>? = null,
        exceptSpotifyTrackIds: Collection<String>? = null,
    ): QueueTrackCombo? {
        // Does a Youtube match if necessary.
        val trackCount = repos.track.getLibraryTrackCount()
        var triedTracks = 0

        while (triedTracks < trackCount) {
            val combos = repos.track.listRandomLibraryTrackCombos(
                limit = 10,
                exceptTrackIds = exceptTrackIds,
                exceptSpotifyTrackIds = exceptSpotifyTrackIds,
            )

            for (combo in combos) {
                getQueueTrackCombo(trackCombo = combo, matchIfNeeded = true)?.also { return it }
                triedTracks++
            }
        }

        return null
    }

    private suspend fun initializeRadio(radio: RadioCombo, context: Context) {
        /** Run the first time a radio is started (i.e. not every time it's REstarted). */
        val channel = Channel<QueueTrackCombo?>(capacity = Channel.BUFFERED)
        val recommendations = getInitialRadioRecommendations(radio)

        if (recommendations == null) {
            SnackbarEngine.addError(
                if (radio.title != null)
                    context.getString(R.string.could_not_start_radio_for_x, radio.title).umlautify()
                else context.getString(R.string.could_not_start_radio).umlautify()
            )
        } else {
            withContext(Dispatchers.Main) {
                repos.player.activateRadio(
                    radio = radio,
                    channel = channel,
                    callbacks = getRadioCallbacks(radioId = radio.id, radioType = radio.type, channel = channel),
                    clearAndPlay = true,
                )
            }

            enqueueRadioTracksJob = launchOnIOThread {
                enqueueRadioTracks(
                    radioId = radio.id,
                    radioType = radio.type,
                    recommendations = recommendations,
                    channel = channel,
                    limit = 20,
                )
            }
        }
    }

    private suspend fun listRandomLibrarySpotifyTrackIds(limit: Int): List<String> {
        /**
         * Returns the Spotify ID's of up to `limit` random tracks from the library. To be used as seed for Spotify's
         * track recommendation API when "library radio" is activated.
         */
        val trackCount = repos.track.getLibraryTrackCount()
        val spotifyTrackIds = mutableListOf<String>()
        var triedTrackCount = 0
        val trueLimit = min(limit, trackCount)

        while (spotifyTrackIds.size < trueLimit && triedTrackCount < trackCount) {
            for (trackCombo in repos.track.listRandomLibraryTrackCombos(min(limit * 4, trackCount))) {
                if (trackCombo.track.spotifyId != null) spotifyTrackIds.add(trackCombo.track.spotifyId)
                else {
                    val spotifyTrack = repos.spotify.matchTrack(
                        track = trackCombo.track,
                        album = trackCombo.album,
                        artists = trackCombo.artists.toArtists()
                            .let { artists -> trackCombo.albumArtist?.let { artists.plus(Artist(it)) } ?: artists },
                    )
                    if (spotifyTrack != null) {
                        spotifyTrackIds.add(spotifyTrack.id)
                        launchOnIOThread { repos.track.setTrackSpotifyId(trackCombo.track.trackId, spotifyTrack.id) }
                    }
                }

                triedTrackCount++
                if (spotifyTrackIds.size == trueLimit) return spotifyTrackIds.toList()
            }
        }

        return spotifyTrackIds.toList()
    }

    private suspend fun restartRadio(radio: RadioCombo) {
        val channel = Channel<QueueTrackCombo?>(capacity = Channel.BUFFERED)

        withContext(Dispatchers.Main) {
            repos.player.activateRadio(
                radio = radio,
                channel = channel,
                callbacks = getRadioCallbacks(radioId = radio.id, radioType = radio.type, channel = channel),
                clearAndPlay = false,
            )
        }
        channel.trySend(null)
    }

    private suspend fun spotifyTrackToQueueTrackCombo(spotifyTrack: SpotifyTrack): QueueTrackCombo? {
        return repos.youtube.getBestTrackMatch(
            trackCombo = spotifyTrack.toTrackCombo(
                getArtist = { repos.artist.artistCache.get(it) },
                isInLibrary = false,
            ),
            albumArtists = spotifyTrack.album.artists.map {
                repos.artist.artistCache.get(BaseArtist(name = it.name, spotifyId = it.id))
            },
            withMetadata = true,
        )?.also {
            repos.track.upsertTrack(it.track)
            repos.artist.insertTrackArtists(it.artists.toTrackArtists())
        }?.toQueueTrackCombo()
    }

    private suspend fun updateGenreList() {
        /** Fetches Musicbrainz' complete genre list. */
        try {
            val existingGenreNames = repos.album.listTags().map { it.name }.toSet()
            val mbGenreNames = repos.musicBrainz.listAllGenreNames()
            val newTags = mbGenreNames
                .minus(existingGenreNames)
                .map { Tag(name = it, isMusicBrainzGenre = true) }

            if (newTags.isNotEmpty()) repos.album.insertTags(newTags)
        } catch (e: Exception) {
            logError("updateGenreList: $e", e)
        }
    }


    /** OVERRIDDEN METHODS ***************************************************/
    override fun onPlayerError(
        error: PlaybackException,
        currentCombo: QueueTrackCombo?,
        lastAction: PlayerRepository.LastAction,
    ) {
        launchOnIOThread {
            if (
                error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS &&
                currentCombo != null &&
                (currentCombo.track.youtubeVideo?.metadataRefreshNeeded == true)
            ) {
                val track = ensureTrackMetadata(currentCombo.track, forceReload = true)
                val playUri = track.playUri

                if (playUri != null && playUri != currentCombo.uri) {
                    withContext(Dispatchers.Main) {
                        repos.player.updateTrack(currentCombo.copy(track = track, uri = playUri))
                        if (lastAction == PlayerRepository.LastAction.PLAY) repos.player.play(currentCombo.position)
                    }
                    // The rest of the album probably has outdated URLs, too:
                    currentCombo.album?.albumId?.also { albumId ->
                        repos.album.getAlbumWithTracks(albumId)?.trackCombos?.forEach {
                            if (it.track.trackId != currentCombo.track.trackId) {
                                ensureTrackMetadata(it.track, forceReload = true)
                            }
                        }
                    }
                    return@launchOnIOThread
                }
            }

            if (lastAction == PlayerRepository.LastAction.PLAY) {
                SnackbarEngine.addError(error.toString())
                withContext(Dispatchers.Main) { repos.player.stop() }
            }
        }
    }
}
