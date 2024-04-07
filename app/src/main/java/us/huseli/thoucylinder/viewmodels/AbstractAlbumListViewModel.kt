package us.huseli.thoucylinder.viewmodels

import android.content.Context
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import us.huseli.retaintheme.extensions.listItemsBetween
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.AlbumDownloadTask
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.launchOnIOThread
import us.huseli.thoucylinder.repositories.Repositories
import us.huseli.thoucylinder.umlautify

abstract class AbstractAlbumListViewModel(
    private val selectionKey: String,
    private val repos: Repositories,
) : AbstractTrackListViewModel(selectionKey, repos) {
    private val _albumDownloadStates = MutableStateFlow<ImmutableList<AlbumDownloadTask.ViewState>>(persistentListOf())
    protected val selectedAlbumIds: StateFlow<List<String>> = repos.album.flowSelectedAlbumIds(selectionKey)

    abstract val albumViewStates: Flow<ImmutableList<Album.ViewState>>

    val albumDownloadStates = _albumDownloadStates.asStateFlow()
    val filteredSelectedAlbumIds: Flow<ImmutableList<String>>
        get() = combine(albumViewStates, selectedAlbumIds) { states, albumIds ->
            albumIds.filter { albumId -> states.map { it.album.albumId }.contains(albumId) }.toImmutableList()
        }

    init {
        launchOnIOThread {
            repos.youtube.albumDownloadTasks.collect { tasks ->
                tasks.forEach { task ->
                    task.viewState.filterNotNull().collect { state ->
                        _albumDownloadStates.value = _albumDownloadStates.value.toMutableList().run {
                            removeIf { it.albumId == state.albumId }
                            add(state)
                            toImmutableList()
                        }
                    }
                }
            }
        }
    }

    open fun onAllAlbumIds(callback: (Collection<String>) -> Unit) {
        launchOnIOThread { callback(albumViewStates.first().map { it.album.albumId }) }
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
                    .toImmutableList()
            )
        }
    }

    fun enqueueAlbum(albumId: String, context: Context) = launchOnIOThread {
        val queueTrackCombos = getQueueTrackCombos(repos.track.listTrackCombosByAlbumId(albumId))

        if (queueTrackCombos.isNotEmpty()) withContext(Dispatchers.Main) {
            repos.player.insertNext(queueTrackCombos)
            SnackbarEngine.addInfo(context.getString(R.string.the_album_was_enqueued_next).umlautify())
        }
    }

    open fun getAlbumSelectionCallbacks(appCallbacks: AppCallbacks, context: Context) = AlbumSelectionCallbacks(
        onAddToPlaylistClick = {
            onSelectedAlbumTracks { appCallbacks.onAddToPlaylistClick(Selection(tracks = it.toImmutableList())) }
        },
        onPlayClick = { onSelectedAlbumsWithTracks { playAlbums(it) } },
        onEnqueueClick = { onSelectedAlbumsWithTracks { enqueueAlbums(it, context) } },
        onUnselectAllClick = { repos.album.unselectAllAlbumIds(selectionKey) },
        onSelectAllClick = { onAllAlbumIds { repos.album.selectAlbumIds(selectionKey, it) } },
        onDeleteClick = { onSelectedAlbums { appCallbacks.onDeleteAlbumsClick(it) } },
    )

    fun onAlbumTracks(albumId: String, callback: (ImmutableList<Track>) -> Unit) {
        launchOnIOThread { callback(repos.track.listTracksByAlbumId(albumId)) }
    }

    fun playAlbum(albumId: String) = launchOnIOThread {
        val queueTrackCombos = getQueueTrackCombos(repos.track.listTrackCombosByAlbumId(albumId))

        if (queueTrackCombos.isNotEmpty()) withContext(Dispatchers.Main) {
            repos.player.replaceAndPlay(queueTrackCombos)
        }
    }

    fun selectAlbumsFromLastSelected(to: String, allAlbumIds: List<String>) = launchOnIOThread {
        val albumIds = filteredSelectedAlbumIds.first().lastOrNull()
            ?.let { allAlbumIds.listItemsBetween(it, to).plus(to) }
            ?: listOf(to)

        repos.album.selectAlbumIds(selectionKey, albumIds)
    }

    fun toggleAlbumSelected(albumId: String) = repos.album.toggleAlbumIdSelected(selectionKey, albumId)


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

    private fun onSelectedAlbums(callback: (Collection<Album.ViewState>) -> Unit) {
        launchOnIOThread {
            val selectedAlbumIds = filteredSelectedAlbumIds.first()
            callback(albumViewStates.first().filter { selectedAlbumIds.contains(it.album.albumId) })
        }
    }

    private fun playAlbums(albumCombos: Collection<AlbumWithTracksCombo>) = launchOnIOThread {
        val queueTrackCombos = getQueueTrackCombos(albumCombos.flatMap { it.trackCombos })

        if (queueTrackCombos.isNotEmpty()) withContext(Dispatchers.Main) {
            repos.player.replaceAndPlay(queueTrackCombos)
        }
    }
}
