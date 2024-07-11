package us.huseli.thoucylinder

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import us.huseli.thoucylinder.dataclasses.album.Album
import us.huseli.thoucylinder.dataclasses.artist.AlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.TrackArtistCredit
import us.huseli.thoucylinder.dataclasses.radio.RadioCombo
import us.huseli.thoucylinder.dataclasses.radio.RadioTrackCombo
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyTrack
import us.huseli.thoucylinder.dataclasses.track.QueueTrackCombo
import us.huseli.thoucylinder.dataclasses.track.Track
import us.huseli.thoucylinder.enums.RadioType
import us.huseli.thoucylinder.interfaces.ILogger
import us.huseli.thoucylinder.repositories.Repositories
import kotlin.math.min
import kotlin.random.Random

class RadioTrackChannel(val radio: RadioCombo, private val repos: Repositories) : AbstractScopeHolder(), ILogger {
    private val usedSpotifyTrackIds = mutableSetOf<String>()
    private val usedLocalTrackIds = mutableSetOf<String>()
    private var fetchLoopJob: Job? = null

    val channel = Channel<RadioTrackCombo>()

    init {
        repos.message.onActivateRadio(radio)
        usedSpotifyTrackIds.addAll(radio.usedSpotifyTrackIds)
        usedLocalTrackIds.addAll(radio.usedLocalTrackIds.filterNotNull())

        launchOnIOThread {
            startLoop()
        }
    }

    fun cancel() {
        fetchLoopJob?.cancel()
        channel.close()
    }


    /** PRIVATE METHODS ***********************************************************************************************/

    private suspend fun enqueueNext(spotifyTrack: suspend () -> SpotifyTrack) {
        var combo: RadioTrackCombo? = null

        while (combo == null) {
            combo =
                if (radio.type == RadioType.LIBRARY && nextIsLibraryTrack()) {
                    getRandomLibraryQueueTrackCombo()
                        ?.let { RadioTrackCombo(queueTrackCombo = it, localId = it.track.trackId) }
                } else {
                    spotifyTrackToQueueTrackCombo(spotifyTrack())
                        ?.let { RadioTrackCombo(queueTrackCombo = it, spotifyId = it.track.spotifyId) }
                }
        }

        channel.send(combo)
        combo.localId?.also { usedLocalTrackIds.add(it) }
        combo.spotifyId?.also { usedSpotifyTrackIds.add(it) }
    }

    private suspend fun getQueueTrackCombo(
        track: Track,
        album: Album? = null,
        albumArtists: Collection<AlbumArtistCredit>,
        trackArtists: Collection<TrackArtistCredit>,
    ): QueueTrackCombo? {
        val newTrack = repos.youtube.ensureTrackPlayUri(
            track = track,
            albumArtists = albumArtists,
            trackArtists = trackArtists,
        ) { repos.track.upsertTrack(it) }

        return newTrack.playUri?.let { uri ->
            QueueTrackCombo(
                track = newTrack,
                uri = uri,
                album = album,
                albumArtists = albumArtists.toList(),
                trackArtists = trackArtists.toList(),
            )
        }
    }

    private suspend fun getRandomLibraryQueueTrackCombo(): QueueTrackCombo? {
        // Does a Youtube match if necessary.
        val trackCount = repos.track.getLibraryTrackCount()
        var triedTracks = 0

        while (triedTracks < trackCount) {
            val combos = repos.track.listRandomLibraryTrackCombos(
                limit = 10,
                exceptTrackIds = usedLocalTrackIds.toList(),
                exceptSpotifyTrackIds = usedSpotifyTrackIds.toList(),
            )

            for (combo in combos) {
                getQueueTrackCombo(
                    track = combo.track,
                    album = combo.album,
                    albumArtists = combo.albumArtists,
                    trackArtists = combo.trackArtists,
                )?.also { return it }
                triedTracks++
            }
        }

        return null
    }

    private suspend fun listRandomLibrarySpotifyTrackIds(limit: Int): List<String> {
        /**
         * Returns the Spotify ID's of up to `limit` random tracks from the library. To be used as seed for Spotify's
         * track recommendation API when "library radio" is activated.
         */
        val trackCount = repos.track.getLibraryTrackCount()
        val spotifyTrackIds = mutableListOf<String>()
        var triedTrackCount = 0
        val trueLimit = limit.coerceAtMost(trackCount)

        while (spotifyTrackIds.size < trueLimit && triedTrackCount < trackCount) {
            for (trackCombo in repos.track.listRandomLibraryTrackCombos(min(limit * 4, trackCount))) {
                if (trackCombo.track.spotifyId != null) spotifyTrackIds.add(trackCombo.track.spotifyId)
                else {
                    val spotifyTrack = repos.spotify.matchTrack(
                        track = trackCombo.track,
                        album = trackCombo.album,
                        artists = trackCombo.trackArtists.plus(trackCombo.albumArtists),
                    )
                    if (spotifyTrack != null) {
                        spotifyTrackIds.add(spotifyTrack.id)
                        repos.track.setTrackSpotifyId(trackCombo.track.trackId, spotifyTrack.id)
                    }
                }
                triedTrackCount++
                if (spotifyTrackIds.size >= trueLimit) return spotifyTrackIds.toList()
            }
        }

        return spotifyTrackIds.toList()
    }

    private fun nextIsLibraryTrack(): Boolean = Random.nextFloat() >= repos.settings.libraryRadioNovelty.value

    private suspend fun spotifyTrackToQueueTrackCombo(spotifyTrack: SpotifyTrack): QueueTrackCombo? {
        val unsavedAlbum = spotifyTrack.album.toAlbum(isLocal = false, isInLibrary = false)
        val trackCombo = spotifyTrack.toTrackCombo(isInLibrary = false, album = unsavedAlbum)
        val track = repos.youtube.getBestTrackMatch(track = trackCombo.track)
        val playUri = track?.playUri

        if (track != null && playUri != null) {
            val album = repos.album.getOrCreateAlbumBySpotifyId(unsavedAlbum, spotifyTrack.album.id)
            val albumArtists =
                trackCombo.albumArtists.map { artistCredit -> artistCredit.withAlbumId(album.albumId) }
            val albumArtistCredits = repos.artist.insertAlbumArtists(albumArtists)
            val updatedTrack = track.copy(albumId = album.albumId).also { repos.track.upsertTrack(it) }
            val trackArtistCredits = repos.artist.insertTrackArtists(trackCombo.trackArtists)

            return QueueTrackCombo(
                track = updatedTrack,
                album = album,
                uri = playUri,
                trackArtists = trackArtistCredits,
                albumArtists = albumArtistCredits,
            )
        }
        return null
    }

    private fun startLoop() {
        fetchLoopJob?.cancel()

        fetchLoopJob = launchOnIOThread {
            try {
                val recommendationsChannel = if (!radio.isInitialized || usedSpotifyTrackIds.size < 5) {
                    when (radio.type) {
                        RadioType.LIBRARY -> repos.spotify.trackRecommendationsChannel(
                            listRandomLibrarySpotifyTrackIds(5)
                        )
                        RadioType.ARTIST -> radio.artist?.let { repos.spotify.trackRecommendationsChannelByArtist(it) }
                        RadioType.ALBUM -> radio.album?.let { album ->
                            repos.album.getAlbumWithTracks(album.albumId)
                                ?.let { repos.spotify.trackRecommendationsChannelByAlbumCombo(it) }
                        }
                        RadioType.TRACK -> radio.track?.let { track ->
                            val albumCombo = track.albumId?.let { repos.album.getAlbumCombo(it) }
                            val artists = repos.artist.listArtistsByTrackId(track.trackId)
                                .plus(albumCombo?.artists ?: emptyList())

                            repos.spotify.trackRecommendationsChannelByTrack(track, radio.album, artists)
                        }
                    }
                } else repos.spotify.trackRecommendationsChannel(usedSpotifyTrackIds)

                while (recommendationsChannel != null) {
                    enqueueNext { recommendationsChannel.receive() }
                }
            } catch (e: ClosedReceiveChannelException) {
                channel.close()
            } catch (e: Throwable) {
                logError(e)
            }
        }
    }
}
