package us.huseli.thoucylinder.viewmodels

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.thoucylinder.dataclasses.album.AlbumSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.album.IAlbumUiState
import us.huseli.thoucylinder.dataclasses.callbacks.AppDialogCallbacks
import us.huseli.thoucylinder.dataclasses.track.TrackUiState
import us.huseli.thoucylinder.listItemsBetween
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import us.huseli.thoucylinder.sortedLike

abstract class AbstractAlbumListViewModel<T : IAlbumUiState>(
    private val selectionKey: String,
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractTrackListViewModel<TrackUiState>(selectionKey, repos, managers) {
    protected abstract val baseAlbumUiStates: StateFlow<ImmutableList<T>>
    open val selectedAlbumIds: Flow<List<String>>
        get() = combine(baseAlbumUiStates, repos.album.flowSelectedAlbumIds(selectionKey)) { states, selectedIds ->
            val allIds = states.map { it.id }
            selectedIds.filter { allIds.contains(it) }
        }

    @Suppress("UNCHECKED_CAST")
    val albumUiStates: StateFlow<ImmutableList<T>>
        get() = combine(baseAlbumUiStates, selectedAlbumIds) { states, selectedIds ->
            states
                .map { state -> state.withIsSelected(selectedIds.contains(state.albumId)) as T }
                .toImmutableList()
        }.stateWhileSubscribed(persistentListOf())

    private val filteredSelectedAlbumIds: Flow<ImmutableList<String>>
        get() = combine(baseAlbumUiStates, selectedAlbumIds) { states, selectedIds ->
            selectedIds.filter { albumId -> states.map { it.albumId }.contains(albumId) }
                .sortedLike(states.map { it.albumId })
                .toImmutableList()
        }

    val selectedAlbumCount: StateFlow<Int>
        get() = filteredSelectedAlbumIds.map { it.size }.stateWhileSubscribed(0)

    open fun getAlbumSelectionCallbacks(dialogCallbacks: AppDialogCallbacks) = AlbumSelectionCallbacks(
        onAddToPlaylistClick = { withSelectedAlbumIds(dialogCallbacks.onAddAlbumsToPlaylistClick) },
        onDeleteClick = { withSelectedAlbumIds(dialogCallbacks.onDeleteAlbumsClick) },
        onEnqueueClick = { withSelectedAlbumIds { managers.player.enqueueAlbums(it) } },
        onExportClick = { withSelectedAlbumIds(dialogCallbacks.onExportAlbumsClick) },
        onPlayClick = { withSelectedAlbumIds { managers.player.playAlbums(it) } },
        onSelectAllClick = { repos.album.selectAlbumIds(selectionKey, baseAlbumUiStates.value.map { it.albumId }) },
        onUnselectAllClick = { repos.album.unselectAllAlbumIds(selectionKey) },
    )

    open fun onAlbumLongClick(albumId: String) {
        launchOnIOThread {
            val albumIds = filteredSelectedAlbumIds.first().lastOrNull()
                ?.let { id ->
                    baseAlbumUiStates.value
                        .map { it.albumId }
                        .listItemsBetween(item1 = id, item2 = albumId, key = { it })
                        .plus(albumId)
                } ?: listOf(albumId)

            repos.album.selectAlbumIds(selectionKey, albumIds)
        }
    }

    open fun toggleAlbumSelected(albumId: String) {
        repos.album.toggleAlbumIdSelected(selectionKey, albumId)
    }

    override fun setTrackStateIsSelected(state: TrackUiState, isSelected: Boolean) = state.copy(isSelected = isSelected)

    fun onAlbumClick(albumId: String, default: (String) -> Unit) {
        launchOnIOThread {
            if (isAlbumSelectEnabled()) toggleAlbumSelected(albumId)
            else default(albumId)
        }
    }

    private suspend fun isAlbumSelectEnabled(): Boolean {
        return filteredSelectedAlbumIds.first().isNotEmpty()
    }

    private fun withSelectedAlbumIds(callback: (List<String>) -> Unit) {
        launchOnIOThread { callback(filteredSelectedAlbumIds.first()) }
    }
}
