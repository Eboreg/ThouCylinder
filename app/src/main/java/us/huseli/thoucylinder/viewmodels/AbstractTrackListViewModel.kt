package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackPojo
import us.huseli.thoucylinder.dataclasses.pojos.QueueTrackPojo
import us.huseli.thoucylinder.Repositories
import java.util.UUID

abstract class AbstractTrackListViewModel(
    selectionKey: String,
    private val repos: Repositories,
) : AbstractSelectViewModel(selectionKey, repos) {
    open val trackDownloadTasks = repos.download.tasks

    fun enqueueTrackPojo(pojo: AbstractTrackPojo, context: Context) = enqueueTrackPojos(listOf(pojo), context)

    fun enqueueTrackPojos(pojos: List<AbstractTrackPojo>, context: Context) = viewModelScope.launch {
        repos.player.insertNext(getQueueTrackPojos(pojos, repos.player.nextItemIndex))
        SnackbarEngine.addInfo(
            context.resources.getQuantityString(R.plurals.x_tracks_enqueued_next, pojos.size, pojos.size)
        )
    }

    fun playPlaylist(playlistId: UUID, startTrackId: UUID? = null) = viewModelScope.launch {
        val pojos = getQueueTrackPojos(repos.playlist.listPlaylistTracks(playlistId))
        val startIndex =
            startTrackId?.let { trackId -> pojos.indexOfFirst { it.track.trackId == trackId }.takeIf { it > -1 } } ?: 0

        repos.player.replaceAndPlay(pojos, startIndex = startIndex)
    }

    fun playTrackPojo(pojo: AbstractTrackPojo) = viewModelScope.launch {
        getQueueTrackPojo(pojo, repos.player.nextItemIndex)?.also { repos.player.insertNextAndPlay(it) }
    }

    fun playTrackPojos(pojos: List<AbstractTrackPojo>, startIndex: Int = 0) = viewModelScope.launch {
        repos.player.replaceAndPlay(getQueueTrackPojos(pojos), startIndex)
    }

    protected suspend fun getQueueTrackPojos(
        trackPojos: List<AbstractTrackPojo>,
        startIndex: Int = 0,
    ): List<QueueTrackPojo> {
        var offset = 0

        return trackPojos.mapNotNull { pojo ->
            getQueueTrackPojo(pojo, startIndex + offset)?.also { offset++ }
        }
    }

    private suspend fun getQueueTrackPojo(trackPojo: AbstractTrackPojo, index: Int): QueueTrackPojo? {
        return trackPojo.track.playUri?.let { uri ->
            QueueTrackPojo(
                track = ensureTrackMetadata(trackPojo.track, commit = true),
                uri = uri,
                position = index,
                album = trackPojo.album,
                spotifyTrack = trackPojo.spotifyTrack,
                lastFmTrack = trackPojo.lastFmTrack,
            )
        }
    }
}
