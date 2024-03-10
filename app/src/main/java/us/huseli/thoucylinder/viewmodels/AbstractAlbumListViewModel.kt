package us.huseli.thoucylinder.viewmodels

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import us.huseli.retaintheme.extensions.listItemsBetween
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.launchOnIOThread
import us.huseli.thoucylinder.umlautify
import java.util.UUID

abstract class AbstractAlbumListViewModel(
    private val selectionKey: String,
    private val repos: Repositories,
) : AbstractTrackListViewModel(selectionKey, repos) {
    abstract val albumCombos: Flow<List<AbstractAlbumCombo>>

    protected val selectedAlbumIds: StateFlow<List<UUID>> = repos.album.flowSelectedAlbumIds(selectionKey)

    val albumDownloadTasks = repos.youtube.albumDownloadTasks
    val filteredSelectedAlbumIds: Flow<List<UUID>>
        get() = combine(albumCombos, selectedAlbumIds) { combos, albumIds ->
            albumIds.filter { albumId -> combos.map { it.album.albumId }.contains(albumId) }
        }

    open fun onAllAlbumIds(callback: (Collection<UUID>) -> Unit) {
        launchOnIOThread { callback(albumCombos.first().map { it.album.albumId }) }
    }

    open fun onSelectedAlbumsWithTracks(callback: (Collection<AlbumWithTracksCombo>) -> Unit) {
        launchOnIOThread {
            callback(repos.album.listAlbumsWithTracks(filteredSelectedAlbumIds.first()))
        }
    }

    open fun onSelectedAlbumTracks(callback: (Collection<Track>) -> Unit) {
        launchOnIOThread {
            callback(
                repos.album.listAlbumsWithTracks(filteredSelectedAlbumIds.first())
                    .flatMap { combo -> combo.trackCombos.map { it.track } }
            )
        }
    }

    fun enqueueAlbum(albumId: UUID, context: Context) = launchOnIOThread {
        val queueTrackCombos = getQueueTrackCombos(repos.track.listTrackCombosByAlbumId(albumId))

        if (queueTrackCombos.isNotEmpty()) withContext(Dispatchers.Main) {
            repos.player.insertNext(queueTrackCombos)
            SnackbarEngine.addInfo(context.getString(R.string.the_album_was_enqueued_next).umlautify())
        }
    }

    fun getAlbumSelectionCallbacks(appCallbacks: AppCallbacks, context: Context) = AlbumSelectionCallbacks(
        onAddToPlaylistClick = { onSelectedAlbumTracks { appCallbacks.onAddToPlaylistClick(Selection(tracks = it)) } },
        onPlayClick = { onSelectedAlbumsWithTracks { playAlbums(it) } },
        onEnqueueClick = { onSelectedAlbumsWithTracks { enqueueAlbums(it, context) } },
        onUnselectAllClick = { repos.album.unselectAllAlbumIds(selectionKey) },
        onSelectAllClick = { onAllAlbumIds { repos.album.selectAlbumIds(selectionKey, it) } },
        onDeleteClick = { onSelectedAlbumsWithTracks { appCallbacks.onDeleteAlbumCombosClick(it) } },
    )

    fun playAlbum(albumId: UUID) = launchOnIOThread {
        val queueTrackCombos = getQueueTrackCombos(repos.track.listTrackCombosByAlbumId(albumId))

        if (queueTrackCombos.isNotEmpty()) withContext(Dispatchers.Main) {
            repos.player.replaceAndPlay(queueTrackCombos)
        }
    }

    fun selectAlbumsFromLastSelected(to: UUID, allAlbumIds: List<UUID>) = launchOnIOThread {
        val albumIds = filteredSelectedAlbumIds.first().lastOrNull()
            ?.let { allAlbumIds.listItemsBetween(it, to).plus(to) }
            ?: listOf(to)

        repos.album.selectAlbumIds(selectionKey, albumIds)
    }

    fun toggleAlbumSelected(albumId: UUID) = repos.album.toggleAlbumIdSelected(selectionKey, albumId)


    /** PRIVATE METHODS *******************************************************/
    private fun enqueueAlbums(albumCombos: Collection<AlbumWithTracksCombo>, context: Context) = launchOnIOThread {
        val queueTrackCombos = getQueueTrackCombos(albumCombos.flatMap { it.trackCombos })

        if (queueTrackCombos.isNotEmpty()) {
            withContext(Dispatchers.Main) { repos.player.insertNext(queueTrackCombos) }

            SnackbarEngine.addInfo(
                context.resources
                    .getQuantityString(
                        R.plurals.x_albums_enqueued_next,
                        albumCombos.size,
                        albumCombos.size,
                    )
                    .umlautify()
            )
        }
    }

    private fun playAlbums(albumCombos: Collection<AlbumWithTracksCombo>) = launchOnIOThread {
        val queueTrackCombos = getQueueTrackCombos(albumCombos.flatMap { it.trackCombos })

        if (queueTrackCombos.isNotEmpty()) withContext(Dispatchers.Main) {
            repos.player.replaceAndPlay(queueTrackCombos)
        }
    }
}
