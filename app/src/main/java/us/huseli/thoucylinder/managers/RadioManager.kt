package us.huseli.thoucylinder.managers

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.AbstractScopeHolder
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.callbacks.RadioCallbacks
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Radio
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.RadioPojo
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyTrack
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyTrackRecommendations
import us.huseli.thoucylinder.dataclasses.views.AlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.views.QueueTrackCombo
import us.huseli.thoucylinder.dataclasses.views.RadioCombo
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit
import us.huseli.thoucylinder.dataclasses.views.toTrackArtists
import us.huseli.thoucylinder.enums.RadioState
import us.huseli.thoucylinder.enums.RadioType
import us.huseli.thoucylinder.repositories.Repositories
import us.huseli.thoucylinder.umlautify
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.random.Random

@Singleton
class RadioManager @Inject constructor(
    private val repos: Repositories,
    @ApplicationContext private val context: Context,
) : AbstractScopeHolder() {
    private val _radioPojo = MutableStateFlow<RadioPojo?>(null)
    private val _radioState = MutableStateFlow(RadioState.INACTIVE)

    private var enqueueRadioTracksJob: Job? = null
    private var radioCallbacks: RadioCallbacks? = null
    private var radioHasMoreTracks = true
    private var radioJob: Job? = null
    private val radioUsedLocalTrackIds = mutableListOf<String>()
    private val radioUsedSpotifyTrackIds = mutableListOf<String>()

    val radioPojo = combine(_radioState, _radioPojo) { state, pojo ->
        if (state != RadioState.INACTIVE) pojo else null
    }

    init {
        launchOnMainThread {
            repos.radio.activeRadio.filterNotNull().collect {
                if (it.isInitialized) restartRadio(it)
                else initializeRadio(it)
            }
        }
        launchOnIOThread {
            for (replaced in repos.player.replaceSignal) {
                deactivateRadio()
            }
        }
    }

    fun deactivateRadio() {
        radioJob?.cancel()
        radioJob = null
        _radioState.value = RadioState.INACTIVE
        radioCallbacks?.deactivate?.invoke()
        radioCallbacks = null
    }

    fun startAlbumRadio(albumId: String) {
        launchOnIOThread { repos.radio.setActiveRadio(Radio(albumId = albumId, type = RadioType.ALBUM)) }
    }

    fun startArtistRadio(artistId: String) {
        launchOnIOThread { repos.radio.setActiveRadio(Radio(artistId = artistId, type = RadioType.ARTIST)) }
    }

    fun startLibraryRadio() {
        launchOnIOThread { repos.radio.setActiveRadio(Radio(type = RadioType.LIBRARY)) }
    }

    fun startTrackRadio(trackId: String) {
        launchOnIOThread { repos.radio.setActiveRadio(Radio(trackId = trackId, type = RadioType.TRACK)) }
    }

    private fun activateRadio(
        radio: RadioCombo,
        channel: Channel<QueueTrackCombo?>,
        callbacks: RadioCallbacks,
        clearAndPlay: Boolean,
    ) {
        _radioState.value = RadioState.LOADING
        _radioPojo.value = RadioPojo(type = radio.type, title = radio.title)
        radioCallbacks = callbacks
        if (clearAndPlay) repos.player.clearQueue()
        radioJob?.cancel()

        radioJob = launchOnMainThread {
            launch {
                if (clearAndPlay) channel.receive()?.also {
                    repos.player.insertLastAndPlay(it)
                    _radioState.value = RadioState.LOADED_FIRST
                }
                for (combo in channel) {
                    if (combo != null) {
                        repos.player.insertLast(combo)
                        _radioState.value = RadioState.LOADED_FIRST
                    } else _radioState.value = RadioState.LOADED
                }
            }

            combineTransform(_radioState, repos.player.tracksLeft) { state, tracksLeft ->
                if (state == RadioState.LOADED) emit(tracksLeft)
            }.collect { tracksLeft ->
                if (tracksLeft < 5) {
                    _radioState.value = RadioState.LOADING
                    callbacks.requestMoreTracks()
                }
            }
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
            val nextIsLibraryTrack = Random.nextFloat() >= repos.settings.libraryRadioNovelty.value
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
        radioId: String,
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
                val artists = repos.artist.listTrackArtistCredits(track.trackId)
                    .plus(albumCombo?.artists ?: emptyList())

                repos.spotify.getTrackRecommendationsByTrack(
                    track = track,
                    album = albumCombo?.album,
                    artists = artists,
                    limit = 40,
                )
            }
        }?.also { radioUsedSpotifyTrackIds.addAll(it.tracks.map { track -> track.id }) }

    private suspend fun getQueueTrackCombo(
        track: Track,
        album: Album? = null,
        albumArtists: Collection<AlbumArtistCredit>,
        trackArtists: Collection<TrackArtistCredit>,
    ): QueueTrackCombo? = withContext(Dispatchers.IO) {
        val newTrack = repos.youtube.ensureTrackPlayUri(
            track = track,
            albumArtists = albumArtists,
            trackArtists = trackArtists,
        ) { repos.track.updateTrack(it) }

        newTrack.playUri?.let { uri ->
            QueueTrackCombo(
                track = newTrack,
                uri = uri,
                album = album,
                albumArtists = albumArtists.toList(),
                artists = trackArtists.toList(),
            )
        }
    }

    private fun getRadioCallbacks(
        radioId: String,
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
            if (radioHasMoreTracks) enqueueRadioTracksJob = launchOnIOThread {
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
        exceptTrackIds: Collection<String>? = null,
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
                getQueueTrackCombo(
                    track = combo.track,
                    album = combo.album,
                    albumArtists = combo.albumArtists,
                    trackArtists = combo.artists,
                )?.also { return it }
                triedTracks++
            }
        }

        return null
    }

    private suspend fun initializeRadio(radio: RadioCombo) {
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
            activateRadio(
                radio = radio,
                channel = channel,
                callbacks = getRadioCallbacks(radioId = radio.id, radioType = radio.type, channel = channel),
                clearAndPlay = true,
            )

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
                        artists = trackCombo.artists.plus(trackCombo.albumArtists),
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

    private fun restartRadio(radio: RadioCombo) {
        val channel = Channel<QueueTrackCombo?>(capacity = Channel.BUFFERED)

        activateRadio(
            radio = radio,
            channel = channel,
            callbacks = getRadioCallbacks(radioId = radio.id, radioType = radio.type, channel = channel),
            clearAndPlay = false,
        )
        channel.trySend(null)
    }

    private suspend fun spotifyTrackToQueueTrackCombo(spotifyTrack: SpotifyTrack): QueueTrackCombo? {
        return repos.youtube.getBestTrackMatch(
            trackCombo = spotifyTrack.toTrackCombo(
                getArtist = { repos.artist.artistCache.get(it) },
                isInLibrary = false,
                isLocal = false,
            ),
        )?.also {
            repos.track.upsertTrack(it.track)
            repos.artist.insertTrackArtists(it.artists.toTrackArtists())
        }?.toQueueTrackCombo()
    }
}
