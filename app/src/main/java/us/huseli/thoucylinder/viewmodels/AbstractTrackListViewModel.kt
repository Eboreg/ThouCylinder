package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.combos.QueueTrackCombo
import us.huseli.thoucylinder.Repositories
import java.util.UUID

abstract class AbstractTrackListViewModel(
    selectionKey: String,
    private val repos: Repositories,
) : AbstractSelectViewModel(selectionKey, repos) {
    open val trackDownloadTasks = repos.download.tasks

    fun enqueueTrackCombo(combo: AbstractTrackCombo, context: Context) = enqueueTrackCombos(listOf(combo), context)

    fun enqueueTrackCombos(combos: List<AbstractTrackCombo>, context: Context) = viewModelScope.launch {
        repos.player.insertNext(getQueueTrackCombos(combos, repos.player.nextItemIndex))
        SnackbarEngine.addInfo(
            context.resources.getQuantityString(R.plurals.x_tracks_enqueued_next, combos.size, combos.size)
        )
    }

    fun playPlaylist(playlistId: UUID, startTrackId: UUID? = null) = viewModelScope.launch {
        val combos = getQueueTrackCombos(repos.playlist.listPlaylistTracks(playlistId))
        val startIndex =
            startTrackId?.let { trackId -> combos.indexOfFirst { it.track.trackId == trackId }.takeIf { it > -1 } } ?: 0

        repos.player.replaceAndPlay(combos, startIndex = startIndex)
    }

    fun playTrackCombo(combo: AbstractTrackCombo) = viewModelScope.launch {
        getQueueTrackCombo(combo, repos.player.nextItemIndex)?.also { repos.player.insertNextAndPlay(it) }
    }

    fun playTrackCombos(combos: List<AbstractTrackCombo>, startIndex: Int = 0) = viewModelScope.launch {
        repos.player.replaceAndPlay(getQueueTrackCombos(combos), startIndex)
    }

    protected suspend fun getQueueTrackCombos(
        trackCombos: List<AbstractTrackCombo>,
        startIndex: Int = 0,
    ): List<QueueTrackCombo> {
        var offset = 0

        return trackCombos.mapNotNull { combo ->
            getQueueTrackCombo(combo, startIndex + offset)?.also { offset++ }
        }
    }

    private suspend fun getQueueTrackCombo(trackCombo: AbstractTrackCombo, index: Int): QueueTrackCombo? {
        val track = ensureTrackMetadata(trackCombo.track)

        return track.playUri?.let { uri ->
            QueueTrackCombo(
                track = track,
                uri = uri,
                position = index,
                album = trackCombo.album,
                spotifyTrack = trackCombo.spotifyTrack,
            )
        }
    }
}
