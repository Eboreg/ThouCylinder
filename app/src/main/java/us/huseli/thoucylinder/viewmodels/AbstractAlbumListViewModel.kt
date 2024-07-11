package us.huseli.thoucylinder.viewmodels

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
    open val selectedAlbumIds: StateFlow<List<String>> = repos.album.flowSelectedAlbumIds(selectionKey)

    @Suppress("UNCHECKED_CAST")
    val albumUiStates: StateFlow<ImmutableList<T>>
        get() = combine(baseAlbumUiStates, selectedAlbumIds) { states, selectedIds ->
            states
                .map { state -> state.withIsSelected(selectedIds.contains(state.albumId)) as T }
                .toImmutableList()
        }.stateLazily(persistentListOf())

    private val filteredSelectedAlbumIds: StateFlow<ImmutableList<String>>
        get() = combine(baseAlbumUiStates, selectedAlbumIds) { states, selectedIds ->
            selectedIds.filter { albumId -> states.map { it.albumId }.contains(albumId) }
                .sortedLike(states.map { it.albumId })
                .toImmutableList()
        }.stateEagerly(persistentListOf())

    val selectedAlbumCount: StateFlow<Int>
        get() = filteredSelectedAlbumIds.map { it.size }.stateLazily(0)

    open fun getAlbumSelectionCallbacks(dialogCallbacks: AppDialogCallbacks) = AlbumSelectionCallbacks(
        onAddToPlaylistClick = { dialogCallbacks.onAddAlbumsToPlaylistClick(filteredSelectedAlbumIds.value) },
        onDeleteClick = { dialogCallbacks.onDeleteAlbumsClick(filteredSelectedAlbumIds.value) },
        onEnqueueClick = { managers.player.enqueueAlbums(filteredSelectedAlbumIds.value) },
        onExportClick = { dialogCallbacks.onExportAlbumsClick(filteredSelectedAlbumIds.value) },
        onPlayClick = { managers.player.playAlbums(filteredSelectedAlbumIds.value) },
        onSelectAllClick = { repos.album.selectAlbumIds(selectionKey, baseAlbumUiStates.value.map { it.albumId }) },
        onUnselectAllClick = { repos.album.unselectAllAlbumIds(selectionKey) },
    )

    open fun isAlbumSelectEnabled(): Boolean {
        return filteredSelectedAlbumIds.value.isNotEmpty()
    }

    open fun onAlbumLongClick(albumId: String) {
        val albumIds = filteredSelectedAlbumIds.value.lastOrNull()
            ?.let { id ->
                baseAlbumUiStates.value
                    .map { it.albumId }
                    .listItemsBetween(item1 = id, item2 = albumId, key = { it })
                    .plus(albumId)
            } ?: listOf(albumId)

        repos.album.selectAlbumIds(selectionKey, albumIds)
    }

    open fun toggleAlbumSelected(albumId: String) {
        repos.album.toggleAlbumIdSelected(selectionKey, albumId)
    }

    override fun setTrackStateIsSelected(state: TrackUiState, isSelected: Boolean) = state.copy(isSelected = isSelected)

    fun onAlbumClick(albumId: String, default: (String) -> Unit) {
        if (isAlbumSelectEnabled()) toggleAlbumSelected(albumId)
        else default(albumId)
    }
}
