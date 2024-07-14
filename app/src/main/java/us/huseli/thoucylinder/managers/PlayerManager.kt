package us.huseli.thoucylinder.managers

import androidx.core.net.toUri
import androidx.media3.common.PlaybackException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import us.huseli.thoucylinder.AbstractScopeHolder
import us.huseli.thoucylinder.dataclasses.track.ISavedTrackCombo
import us.huseli.thoucylinder.dataclasses.track.QueueTrackCombo
import us.huseli.thoucylinder.enums.PlaybackState
import us.huseli.thoucylinder.interfaces.ILogger
import us.huseli.thoucylinder.repositories.PlayerRepository
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerManager @Inject constructor(
    private val repos: Repositories,
) : AbstractScopeHolder(), PlayerRepository.Listener, ILogger {
    init {
        repos.player.addListener(this)
    }

    fun enqueueAlbums(albumIds: Collection<String>) {
        launchOnMainThread { enqueueQueueTrackCombos(flowQueueTrackCombosByAlbumIds(albumIds)) }
    }

    fun enqueueArtist(artistId: String) {
        launchOnMainThread { enqueueQueueTrackCombos(flowQueueTrackCombosByArtistId(artistId)) }
    }

    fun enqueueTracks(trackIds: Collection<String>) {
        launchOnMainThread { enqueueQueueTrackCombos(flowQueueTrackCombosByTrackId(trackIds)) }
    }

    fun moveTracksNext(queueTrackIds: Collection<String>) {
        launchOnMainThread {
            if (queueTrackIds.isNotEmpty()) {
                repos.player.moveNext(queueTrackIds)
                repos.message.onEnqueueTracksNext(queueTrackIds.size)
            }
        }
    }

    fun playAlbum(albumId: String, startIndex: Int = 0) {
        launchOnMainThread {
            playQueueTrackCombos(
                combos = flowQueueTrackCombos(
                    trackCombos = repos.track.listTrackCombosByAlbumId(albumId),
                    startIndex = startIndex,
                ),
                firstTrackPos = startIndex,
            )
        }
    }

    fun playAlbums(albumIds: Collection<String>) {
        launchOnMainThread { playQueueTrackCombos(flowQueueTrackCombosByAlbumIds(albumIds)) }
    }

    fun playArtist(artistId: String) {
        launchOnMainThread { playQueueTrackCombos(flowQueueTrackCombosByArtistId(artistId)) }
    }

    fun playPlaylist(playlistId: String, startTrackId: String? = null) {
        launchOnMainThread {
            var index = 0

            flowQueueTrackCombosByPlaylistId(playlistId).collect { combo ->
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

    fun playTrack(trackId: String) {
        launchOnMainThread {
            getQueueTrackComboByTrackId(trackId)?.also { queueTrackCombo ->
                repos.player.insertNextAndPlay(queueTrackCombo)
            }
        }
    }

    fun playTracks(trackIds: Collection<String>) {
        launchOnMainThread { playQueueTrackCombos(flowQueueTrackCombosByTrackId(trackIds)) }
    }

    private suspend fun enqueueQueueTrackCombos(combos: Flow<QueueTrackCombo>) {
        val offset = repos.player.nextItemIndex
        var index = 0

        combos.onCompletion {
            if (index > 0) repos.message.onEnqueueTracksNext(index)
        }.collect { combo ->
            repos.player.insert(combo, index + offset)
            index++
        }
    }

    private fun flowQueueTrackCombos(trackCombos: List<ISavedTrackCombo>, startIndex: Int = 0) = flow {
        if (trackCombos.size > startIndex) {
            getQueueTrackComboByTrackCombo(trackCombos[startIndex])?.also { emit(it) }
        }
        for ((index, combo) in trackCombos.withIndex()) {
            if (index != startIndex) getQueueTrackComboByTrackCombo(combo)?.also { emit(it) }
        }
    }

    private suspend fun flowQueueTrackCombosByAlbumIds(albumIds: Collection<String>) =
        flowQueueTrackCombos(albumIds.flatMap { repos.track.listTrackCombosByAlbumId(it) })

    private suspend fun flowQueueTrackCombosByArtistId(artistId: String) =
        flowQueueTrackCombos(repos.track.listTrackCombosByArtistId(artistId))

    private suspend fun flowQueueTrackCombosByPlaylistId(playlistId: String) =
        flowQueueTrackCombos(repos.playlist.listPlaylistTrackCombos(playlistId))

    private suspend fun flowQueueTrackCombosByTrackId(trackIds: Collection<String>): Flow<QueueTrackCombo> =
        flowQueueTrackCombos(repos.track.listTrackCombosById(trackIds))

    private suspend fun getQueueTrackComboByTrackCombo(combo: ISavedTrackCombo): QueueTrackCombo? {
        val updatedTrack = repos.youtube.ensureTrackPlayUri(
            track = combo.track,
            albumArtists = combo.albumArtists,
            trackArtists = combo.trackArtists,
        ) { repos.track.upsertTrack(it) }

        return updatedTrack.playUri?.let { uri ->
            QueueTrackCombo(
                track = updatedTrack,
                uri = uri,
                album = combo.album,
                albumArtists = combo.albumArtists,
                trackArtists = combo.trackArtists,
            )
        }
    }

    private suspend fun getQueueTrackComboByTrackId(trackId: String): QueueTrackCombo? =
        repos.track.getTrackComboById(trackId)?.let { combo ->
            getQueueTrackComboByTrackCombo(combo)
        }

    private suspend fun playQueueTrackCombos(combos: Flow<QueueTrackCombo>) {
        combos.collectIndexed { index, combo ->
            if (index == 0) repos.player.replaceAndPlay(combo)
            else repos.player.insertLast(combo)
        }
    }

    private suspend fun playQueueTrackCombos(combos: Flow<QueueTrackCombo>, firstTrackPos: Int) {
        /** firstTrackPos = position in the queue where we will put the first track received from the flow. */
        combos.collectIndexed { index, combo ->
            if (index == 0) {
                repos.player.replaceAndPlay(combo)
            } else if (index <= firstTrackPos) {
                repos.player.insert(combo, index - 1)
            } else {
                repos.player.insertLast(combo)
            }
            /*
            if (index == 0) {
                if (firstTrackPos == 0) repos.player.replaceAndPlay(combo)
                else repos.player.replace(combo)
            } else {
                if (index == firstTrackPos) repos.player.insertLastAndPlay(combo)
                else repos.player.insertLast(combo)
            }
             */
        }
    }

    override fun onPlaybackChange(combo: QueueTrackCombo?, state: PlaybackState) {
        if (combo != null && state == PlaybackState.PLAYING) repos.lastFm.sendNowPlaying(combo)
    }

    override fun onHalfTrackPlayed(combo: QueueTrackCombo, startTimestamp: Long) {
        launchOnIOThread {
            val lastFmPlayCount = combo.artistString?.let {
                val response = repos.lastFm.getTrackInfo(title = combo.track.title, artist = it)
                response?.track?.userPlayCount?.toInt()?.plus(1)
            }

            repos.lastFm.sendScrobble(combo, startTimestamp)
            repos.track.setPlayCounts(
                trackId = combo.track.trackId,
                localPlayCount = combo.track.localPlayCount + 1,
                lastFmPlayCount = lastFmPlayCount,
            )
        }
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

        launchOnMainThread {
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
                    onChanged = { repos.track.upsertTrack(it) },
                )
                val playUri = updatedTrack.playUri

                if (playUri != null && playUri != currentCombo.uri) {
                    repos.player.updateTrack(currentCombo.copy(track = updatedTrack, uri = playUri))
                    if (lastAction == PlayerRepository.LastAction.PLAY) repos.player.play(currentCombo.position)
                    // The rest of the album probably has outdated URLs, too:
                    currentCombo.album?.albumId?.also { albumId ->
                        for (track in repos.track.listTracksByAlbumId(albumId)) {
                            if (track.trackId != currentCombo.track.trackId) {
                                repos.youtube.ensureTrackMetadata(
                                    track = track,
                                    onChanged = { repos.track.upsertTrack(it) },
                                )
                            }
                        }
                    }
                    return@launchOnMainThread
                }
            }

            if (lastAction == PlayerRepository.LastAction.PLAY) {
                repos.message.onPlayerError(error)
                repos.player.stop()
            }
        }
    }
}
