package us.huseli.thoucylinder.viewmodels

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import us.huseli.retaintheme.extensions.listItemsBetween
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.combos.QueueTrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.launchOnIOThread
import us.huseli.thoucylinder.launchOnMainThread
import us.huseli.thoucylinder.umlautify
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

abstract class AbstractTrackListViewModel(
    private val selectionKey: String,
    private val repos: Repositories,
) : AbstractBaseViewModel(repos) {
    val selectedTrackIds: StateFlow<List<UUID>> = repos.track.flowSelectedTrackIds(selectionKey)
    val latestSelectedTrackId = selectedTrackIds.map { it.lastOrNull() }
    open val trackDownloadTasks = repos.download.tasks

    open fun enqueueSelectedTracks(context: Context) {
        launchOnIOThread { enqueueTrackCombos(listSelectedTrackCombos(), context) }
    }

    open suspend fun listSelectedTrackCombos(): List<AbstractTrackCombo> =
        repos.track.listTrackCombosById(selectedTrackIds.value)

    open suspend fun listSelectedTracks(): List<Track> = repos.track.listTracksById(selectedTrackIds.value)

    open fun playSelectedTracks() {
        launchOnIOThread { playTrackCombos(listSelectedTrackCombos()) }
    }

    fun enqueueTrackCombo(combo: AbstractTrackCombo, context: Context) = enqueueTrackCombos(listOf(combo), context)

    fun enqueueTrackCombos(combos: Collection<AbstractTrackCombo>, context: Context) = launchOnMainThread {
        repos.player.insertNext(getQueueTrackCombos(combos, repos.player.nextItemIndex))
        SnackbarEngine.addInfo(
            context.resources.getQuantityString(R.plurals.x_tracks_enqueued_next, combos.size, combos.size).umlautify()
        )
    }

    fun getTrackSelectionCallbacks(appCallbacks: AppCallbacks, context: Context) = TrackSelectionCallbacks(
        onAddToPlaylistClick = {
            launchOnIOThread { appCallbacks.onAddToPlaylistClick(Selection(tracks = listSelectedTracks())) }
        },
        onPlayClick = { playSelectedTracks() },
        onEnqueueClick = { enqueueSelectedTracks(context) },
        onUnselectAllClick = { unselectAllTracks() },
    )

    fun playPlaylist(playlistId: UUID, startTrackId: UUID? = null) = launchOnMainThread {
        val combos = getQueueTrackCombos(repos.playlist.listPlaylistTrackCombos(playlistId))
        val startIndex =
            startTrackId?.let { trackId -> combos.indexOfFirst { it.track.trackId == trackId }.takeIf { it > -1 } } ?: 0

        repos.player.replaceAndPlay(combos, startIndex = startIndex)
    }

    fun playTrackCombo(combo: AbstractTrackCombo) = launchOnIOThread {
        getQueueTrackCombo(combo, repos.player.nextItemIndex)?.also {
            withContext(Dispatchers.Main) {
                repos.player.insertNextAndPlay(it)
            }
        }
    }

    fun playTrackCombos(combos: Collection<AbstractTrackCombo>, startIndex: Int = 0) = launchOnMainThread {
        repos.player.replaceAndPlay(withContext(Dispatchers.IO) { getQueueTrackCombos(combos) }, startIndex)
    }

    fun selectTracksBetweenIndices(fromIndex: Int?, toIndex: Int, getTrackIdAtIndex: (Int) -> UUID?) {
        val trackIds = if (fromIndex != null)
            (min(fromIndex, toIndex)..max(fromIndex, toIndex)).mapNotNull { getTrackIdAtIndex(it) }
        else getTrackIdAtIndex(toIndex)?.let { listOf(it) }

        if (trackIds != null) repos.track.selectTrackIds(selectionKey, trackIds)
    }

    fun selectTracksFromLastSelected(to: UUID, allTrackIds: List<UUID>) {
        val trackIds = selectedTrackIds.value.lastOrNull()
            ?.let { allTrackIds.listItemsBetween(it, to).plus(to) }
            ?: listOf(to)

        repos.track.selectTrackIds(selectionKey, trackIds)
    }

    fun toggleTrackSelected(trackId: UUID) {
        repos.track.toggleTrackIdSelected(selectionKey, trackId)
    }

    fun unselectAllTracks() = repos.track.unselectAllTrackIds(selectionKey)

    fun unselectTracks(trackIds: Collection<UUID>) = repos.track.unselectTrackIds(selectionKey, trackIds)

    private suspend fun getQueueTrackCombo(trackCombo: AbstractTrackCombo, index: Int): QueueTrackCombo? {
        val track = ensureTrackMetadata(trackCombo.track)

        return track.playUri?.let { uri ->
            QueueTrackCombo(
                track = track,
                uri = uri,
                position = index,
                album = trackCombo.album,
                albumArtist = trackCombo.albumArtist,
                artists = trackCombo.artists,
            )
        }
    }

    protected suspend fun getQueueTrackCombos(
        trackCombos: Collection<AbstractTrackCombo>,
        startIndex: Int = 0,
    ): List<QueueTrackCombo> {
        var offset = 0

        return trackCombos.mapNotNull { combo ->
            getQueueTrackCombo(combo, startIndex + offset)?.also { offset++ }
        }
    }
}
