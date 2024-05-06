package us.huseli.thoucylinder.viewmodels

import android.content.Context
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.retaintheme.extensions.listItemsBetween
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.uistates.AlbumUiState
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories

abstract class AbstractAlbumListViewModel(
    private val selectionKey: String,
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractTrackListViewModel(selectionKey, repos, managers) {
    private val selectModeEnabled: StateFlow<Boolean>
        get() = filteredSelectedAlbumIds.map { it.isNotEmpty() }.distinctUntilChanged().stateEagerly(false)

    protected val selectedAlbumIds: StateFlow<List<String>> = repos.album.flowSelectedAlbumIds(selectionKey)

    abstract val albumUiStates: StateFlow<ImmutableList<AlbumUiState>>

    val filteredSelectedAlbumIds: StateFlow<ImmutableList<String>>
        get() = combine(albumUiStates, selectedAlbumIds) { states, albumIds ->
            albumIds.filter { albumId -> states.map { it.albumId }.contains(albumId) }.toImmutableList()
        }.stateEagerly(persistentListOf())

    fun onAlbumClick(albumId: String, default: ((String) -> Unit)? = null) {
        if (selectModeEnabled.value) toggleAlbumSelected(albumId)
        else default?.invoke(albumId)
    }

    fun onAlbumLongClick(albumId: String) {
        launchOnIOThread {
            val albumIds = filteredSelectedAlbumIds.value.lastOrNull()
                ?.let { id ->
                    albumUiStates.value.map { it.albumId }
                        .listItemsBetween(id, albumId)
                        .plus(albumId)
                }
                ?: listOf(albumId)

            repos.album.selectAlbumIds(selectionKey, albumIds)
        }
    }

    open fun onAllAlbumIds(callback: (Collection<String>) -> Unit) {
        callback(albumUiStates.value.map { it.albumId })
    }

    open fun onSelectedAlbumsWithTracks(callback: (Collection<AlbumWithTracksCombo>) -> Unit) {
        launchOnIOThread { callback(repos.album.listAlbumsWithTracks(filteredSelectedAlbumIds.value)) }
    }

    open fun onSelectedAlbumTracks(callback: (Collection<Track>) -> Unit) {
        launchOnIOThread {
            callback(
                repos.album.listAlbumsWithTracks(filteredSelectedAlbumIds.value)
                    .flatMap { combo -> combo.trackCombos.map { it.track } }
                    .toImmutableList()
            )
        }
    }

    open fun getAlbumSelectionCallbacks(appCallbacks: AppCallbacks, context: Context) = AlbumSelectionCallbacks(
        onAddToPlaylistClick = { appCallbacks.onAddAlbumsToPlaylistClick(filteredSelectedAlbumIds.value) },
        onPlayClick = { managers.player.playAlbums(filteredSelectedAlbumIds.value) },
        onEnqueueClick = { managers.player.enqueueAlbums(filteredSelectedAlbumIds.value) },
        onUnselectAllClick = { repos.album.unselectAllAlbumIds(selectionKey) },
        onSelectAllClick = { onAllAlbumIds { repos.album.selectAlbumIds(selectionKey, it) } },
        onDeleteClick = { appCallbacks.onDeleteAlbumsClick(filteredSelectedAlbumIds.value) },
    )

    fun selectAlbumsFromLastSelected(to: String) {
        launchOnIOThread {
            val albumIds = filteredSelectedAlbumIds.value.lastOrNull()
                ?.let { id -> albumUiStates.value.map { it.albumId }.listItemsBetween(id, to).plus(to) }
                ?: listOf(to)

            repos.album.selectAlbumIds(selectionKey, albumIds)
        }
    }

    fun toggleAlbumSelected(albumId: String) = repos.album.toggleAlbumIdSelected(selectionKey, albumId)
}
