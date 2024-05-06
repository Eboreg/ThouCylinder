package us.huseli.thoucylinder.managers

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.PlaybackException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.AbstractScopeHolder
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.uistates.TrackUiState
import us.huseli.thoucylinder.dataclasses.views.AlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.views.QueueTrackCombo
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit
import us.huseli.thoucylinder.enums.PlaybackState
import us.huseli.thoucylinder.interfaces.ILogger
import us.huseli.thoucylinder.interfaces.PlayerRepositoryListener
import us.huseli.thoucylinder.repositories.PlayerRepository
import us.huseli.thoucylinder.repositories.Repositories
import us.huseli.thoucylinder.umlautify
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerManager @Inject constructor(
    private val repos: Repositories,
    @ApplicationContext private val context: Context,
) : AbstractScopeHolder(), PlayerRepositoryListener, ILogger {
    init {
        repos.player.addListener(this)
    }

    fun enqueueAlbums(albumIds: Collection<String>, onFinish: (Int) -> Unit = {}) {
        launchOnIOThread { enqueueQueueTrackCombos(flowQueueTrackCombosByAlbumId(albumIds), onFinish) }
    }

    fun enqueueArtist(artistId: String, onFinish: (Int) -> Unit = {}) {
        launchOnIOThread { enqueueQueueTrackCombos(flowQueueTrackCombosByArtistId(artistId), onFinish) }
    }

    fun enqueueTrackCombo(trackCombo: AbstractTrackCombo, onFinish: (QueueTrackCombo) -> Unit = {}) {
        launchOnIOThread { getQueueTrackComboByTrackCombo(trackCombo)?.also { enqueueQueueTrackCombo(it, onFinish) } }
    }

    fun enqueueTrackCombos(trackCombos: Collection<AbstractTrackCombo>, onFinish: (Int) -> Unit = {}) {
        launchOnIOThread { enqueueQueueTrackCombos(flowQueueTrackCombos(trackCombos), onFinish) }
    }

    fun enqueueTracks(trackIds: Collection<String>, onFinish: (Int) -> Unit = {}) {
        launchOnIOThread { enqueueQueueTrackCombos(flowQueueTrackCombosByTrackId(trackIds), onFinish) }
    }

    fun enqueueTrackUiState(state: TrackUiState, onFinish: (QueueTrackCombo) -> Unit = {}) {
        launchOnIOThread { getQueueTrackComboByUiState(state)?.also { enqueueQueueTrackCombo(it, onFinish) } }
    }

    fun moveTracksNext(queueTrackIds: Collection<String>, onFinish: (Int) -> Unit = {}) {
        launchOnMainThread {
            if (queueTrackIds.isNotEmpty()) {
                repos.player.moveNext(queueTrackIds)
                SnackbarEngine.addInfo(
                    message = context.resources.getQuantityString(
                        R.plurals.x_tracks_enqueued_next,
                        queueTrackIds.size,
                        queueTrackIds.size,
                    ).umlautify(),
                )
            }
            onFinish(queueTrackIds.size)
        }
    }

    fun playAlbum(albumId: String, startIndex: Int = 0, onFinish: (Int) -> Unit = {}) {
        launchOnIOThread { playQueueTrackCombos(flowQueueTrackCombosByAlbumId(listOf(albumId)), startIndex, onFinish) }
    }

    fun playAlbums(albumIds: Collection<String>, onFinish: (Int) -> Unit = {}) {
        launchOnIOThread { playQueueTrackCombos(flowQueueTrackCombosByAlbumId(albumIds), 0, onFinish) }
    }

    fun playArtist(artistId: String, onFinish: (Int) -> Unit = {}) {
        launchOnIOThread { playQueueTrackCombos(flowQueueTrackCombosByArtistId(artistId), 0, onFinish) }
    }

    fun playPlaylist(playlistId: String, startTrackId: String? = null, onFinish: (Int) -> Unit = {}) {
        launchOnIOThread {
            var index = 0

            flowQueueTrackCombosByPlaylistId(playlistId).onCompletion { onFinish(index) }.collect { combo ->
                withContext(Dispatchers.Main) {
                    if (index == 0) {
                        if (combo.track.trackId == startTrackId || startTrackId == null) repos.player.replaceAndPlay(
                            combo
                        )
                        else repos.player.replace(combo)
                    } else {
                        if (combo.track.trackId == startTrackId) repos.player.insertLastAndPlay(combo)
                        else repos.player.insertLast(combo)
                    }
                    index++
                }
            }
        }
    }

    fun playTrackCombo(trackCombo: AbstractTrackCombo, onFinish: () -> Unit = {}) {
        launchOnIOThread {
            getQueueTrackComboByTrackCombo(trackCombo)?.also {
                withContext(Dispatchers.Main) { repos.player.insertNextAndPlay(it) }
                onFinish()
            }
        }
    }

    fun playTrackCombos(
        trackCombos: Collection<AbstractTrackCombo>,
        startIndex: Int = 0,
        onFinish: (Int) -> Unit = {},
    ) {
        launchOnIOThread { playQueueTrackCombos(flowQueueTrackCombos(trackCombos), startIndex, onFinish) }
    }

    fun playTracks(
        trackIds: Collection<String>,
        startIndex: Int = 0,
        onFinish: (Int) -> Unit = {},
    ) {
        launchOnIOThread { playQueueTrackCombos(flowQueueTrackCombosByTrackId(trackIds), startIndex, onFinish) }
    }

    fun playTrackUiState(state: TrackUiState, onFinish: (QueueTrackCombo) -> Unit = {}) {
        launchOnIOThread {
            getQueueTrackComboByUiState(state)?.also { queueTrackCombo ->
                withContext(Dispatchers.Main) { repos.player.insertNextAndPlay(queueTrackCombo) }
                onFinish(queueTrackCombo)
            }
        }
    }

    private suspend fun enqueueQueueTrackCombo(combo: QueueTrackCombo, onFinish: (QueueTrackCombo) -> Unit) {
        withContext(Dispatchers.Main) { repos.player.insertNext(combo) }
        SnackbarEngine.addInfo(
            message = context.getString(R.string.x_was_enqueued_next, combo.track.trackId).umlautify(),
        )
        onFinish(combo)
    }

    private suspend fun enqueueQueueTrackCombos(combos: Flow<QueueTrackCombo>, onFinish: (Int) -> Unit) {
        val offset = withContext(Dispatchers.Main) { repos.player.nextItemIndex }
        var index = 0

        combos.onCompletion {
            if (index > 0) SnackbarEngine.addInfo(
                message = context.resources.getQuantityString(R.plurals.x_tracks_enqueued_next, index, index)
                    .umlautify(),
            )
            onFinish(index)
        }.collect { combo ->
            withContext(Dispatchers.Main) {
                repos.player.insert(combo, index + offset)
                index++
            }
        }
    }

    private fun flowQueueTrackCombos(trackCombos: Collection<AbstractTrackCombo>) = flow {
        for (combo in trackCombos) {
            getQueueTrackComboByTrackCombo(combo)?.also { emit(it) }
        }
    }

    private suspend fun flowQueueTrackCombosByAlbumId(albumIds: Collection<String>) =
        flowQueueTrackCombos(albumIds.flatMap { repos.track.listTrackCombosByAlbumId(it) })

    private suspend fun flowQueueTrackCombosByArtistId(artistId: String) =
        flowQueueTrackCombos(repos.track.listTrackCombosByArtistId(artistId))

    private suspend fun flowQueueTrackCombosByPlaylistId(playlistId: String) =
        flowQueueTrackCombos(repos.playlist.listPlaylistTrackCombos(playlistId))

    private suspend fun flowQueueTrackCombosByTrackId(trackIds: Collection<String>): Flow<QueueTrackCombo> =
        flowQueueTrackCombos(repos.track.listTrackCombosById(trackIds))

    private suspend fun getQueueTrackComboByTrackCombo(combo: AbstractTrackCombo) = getQueueTrackCombo(
        track = combo.track,
        album = combo.album,
        albumArtists = combo.albumArtists,
        trackArtists = combo.artists,
    )

    private suspend fun getQueueTrackComboByUiState(state: TrackUiState): QueueTrackCombo? =
        repos.track.getTrackComboById(state.trackId)?.let { combo ->
            getQueueTrackComboByTrackCombo(combo)
        }

    private suspend fun getQueueTrackCombo(
        track: Track,
        album: Album? = null,
        albumArtists: List<AlbumArtistCredit>? = null,
        trackArtists: List<TrackArtistCredit>? = null,
    ): QueueTrackCombo? {
        val updatedTrack = repos.youtube.ensureTrackPlayUri(
            track = track,
            albumArtists = albumArtists,
            trackArtists = trackArtists,
        ) { repos.track.updateTrack(it) }

        return updatedTrack.playUri?.let { uri ->
            QueueTrackCombo(
                track = updatedTrack,
                uri = uri,
                album = album,
                albumArtists = albumArtists ?: emptyList(),
                artists = trackArtists ?: emptyList(),
            )
        }
    }

    private suspend fun playQueueTrackCombos(combos: Flow<QueueTrackCombo>, startIndex: Int, onFinish: (Int) -> Unit) {
        var index = 0

        combos.onCompletion { onFinish(index) }.collect { combo ->
            withContext(Dispatchers.Main) {
                if (index == 0) {
                    if (startIndex == 0) repos.player.replaceAndPlay(combo)
                    else repos.player.replace(combo)
                } else {
                    if (index == startIndex) repos.player.insertLastAndPlay(combo)
                    else repos.player.insertLast(combo)
                }
                index++
            }
        }
    }

    override suspend fun onPlaybackChange(combo: QueueTrackCombo?, state: PlaybackState) {
        val artistString = combo?.artists?.joined()

        if (combo != null && artistString != null && state == PlaybackState.PLAYING) {
            repos.lastFm.sendNowPlaying(combo, artistString)
        }
    }

    override suspend fun onHalfTrackPlayed(combo: QueueTrackCombo, startTimestamp: Long) {
        val artistString = combo.artists.joined()

        if (artistString != null) repos.lastFm.sendScrobble(combo, artistString, startTimestamp)
    }

    override fun onPlayerError(
        error: PlaybackException,
        currentCombo: QueueTrackCombo?,
        lastAction: PlayerRepository.LastAction,
    ) {
        logError(
            "onPlayerError: errorCode=${error.errorCode}, currentCombo=$currentCombo, lastAction=$lastAction",
            error,
        )

        launchOnIOThread {
            val url = currentCombo?.track?.youtubeVideo?.metadata?.url
            val uri = currentCombo?.uri
            val urlExpires = (url ?: uri)?.toUri()?.getQueryParameter("expire")?.toLong()?.times(1000)

            log("onPlayerError: url=$url, uri=$uri, urlExpires=$urlExpires")

            if (
                error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS &&
                currentCombo != null &&
                currentCombo.metadataRefreshNeeded
            ) {
                val updatedTrack = repos.youtube.ensureTrackMetadata(
                    track = currentCombo.track,
                    forceReload = true,
                    onChanged = { repos.track.updateTrack(it) },
                )
                val playUri = updatedTrack.playUri

                if (playUri != null && playUri != currentCombo.uri) {
                    withContext(Dispatchers.Main) {
                        repos.player.updateTrack(currentCombo.copy(track = updatedTrack, uri = playUri))
                        if (lastAction == PlayerRepository.LastAction.PLAY) repos.player.play(currentCombo.position)
                    }
                    // The rest of the album probably has outdated URLs, too:
                    currentCombo.album?.albumId?.also { albumId ->
                        for (track in repos.track.listTracksByAlbumId(albumId)) {
                            if (track.trackId != currentCombo.track.trackId) {
                                repos.youtube.ensureTrackMetadata(
                                    track = track,
                                    onChanged = { repos.track.updateTrack(it) },
                                )
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
