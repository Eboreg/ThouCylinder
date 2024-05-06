package us.huseli.thoucylinder.viewmodels

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.thoucylinder.Constants.NAV_ARG_PLAYLIST
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.uistates.TrackUiState
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractTrackListViewModel("PlaylistViewModel", repos, managers) {
    private val _trackUiStates = MutableStateFlow<List<TrackUiState>>(emptyList())

    val playlistId: String = savedStateHandle.get<String>(NAV_ARG_PLAYLIST)!!
    val playlist: StateFlow<Playlist?> = repos.playlist.flowPlaylist(playlistId).stateLazily()
    val trackUiStates = _trackUiStates.asStateFlow()

    init {
        launchOnIOThread {
            repos.playlist.flowPlaylistTracks(playlistId).collect { combos ->
                _trackUiStates.value = combos.map { TrackUiState.fromPlaylistTrackCombo(it) }
            }
        }
    }

    suspend fun ensureTrackMetadata(uiState: TrackUiState) = managers.library.ensureTrackMetadata(uiState.trackId)

    suspend fun ensureTrackMetadata(track: Track) = managers.library.ensureTrackMetadata(track)

    suspend fun getTrackUiStateThumbnail(uiState: TrackUiState): ImageBitmap? =
        managers.image.getTrackUiStateThumbnailImageBitmap(uiState)

    fun onMoveTrack(from: Int, to: Int) {
        _trackUiStates.value = _trackUiStates.value.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun onMoveTrackFinished(from: Int, to: Int) {
        launchOnIOThread { repos.playlist.movePlaylistTrack(playlistId, from, to) }
    }

    fun playPlaylist(startAtTrackId: String? = null) = playPlaylist(playlistId, startAtTrackId)

    fun removeTrackCombos(ids: List<String>) {
        launchOnIOThread {
            repos.playlist.removePlaylistTracks(playlistId, ids)
            unselectTracks(ids)
        }
    }

    fun renamePlaylist(newName: String) {
        launchOnIOThread { repos.playlist.renamePlaylist(playlistId, newName) }
    }

    override fun getTrackSelectionCallbacks(appCallbacks: AppCallbacks): TrackSelectionCallbacks =
        super.getTrackSelectionCallbacks(appCallbacks).copy(
            onSelectAllClick = {
                repos.track.selectTrackIds(
                    selectionKey = "PlaylistViewModel",
                    trackIds = _trackUiStates.value.map { it.trackId },
                )
            }
        )

    override suspend fun listSelectedTrackCombos() =
        repos.playlist.listPlaylistTrackCombosById(selectedTrackIds.value).toImmutableList()

    override suspend fun listSelectedTracks() = listSelectedTrackCombos().map { it.track }.toImmutableList()
}
