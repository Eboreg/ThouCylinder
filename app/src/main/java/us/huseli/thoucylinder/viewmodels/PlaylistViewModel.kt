package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.thoucylinder.Constants.NAV_ARG_PLAYLIST
import us.huseli.thoucylinder.dataclasses.playlist.PlaylistUiState
import us.huseli.thoucylinder.dataclasses.track.TrackUiState
import us.huseli.thoucylinder.dataclasses.track.toUiStates
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractTrackListViewModel<TrackUiState>("PlaylistViewModel", repos, managers) {
    private val _isLoadingTracks = MutableStateFlow(true)
    private val _playlistId = MutableStateFlow(savedStateHandle.get<String>(NAV_ARG_PLAYLIST)!!)
    private val _trackUiStates = MutableStateFlow<List<TrackUiState>>(emptyList())

    override val baseTrackUiStates: StateFlow<ImmutableList<TrackUiState>> =
        _trackUiStates.map { it.toImmutableList() }.stateWhileSubscribed(persistentListOf())

    val playlistState: StateFlow<PlaylistUiState?> =
        combine(_playlistId, repos.playlist.playlistUiStates) { playlistId, states ->
            states.find { it.id == playlistId }
        }.stateWhileSubscribed()
    val isLoadingTracks = _isLoadingTracks.asStateFlow()

    init {
        launchOnIOThread {
            _playlistId.collect {
                unselectAllTracks()
            }
        }
        launchOnIOThread {
            repos.playlist.flowPlaylistTracks(_playlistId.value).collect { combos ->
                _trackUiStates.value = combos.toUiStates()
                _isLoadingTracks.value = false
            }
        }
    }

    override fun setTrackStateIsSelected(state: TrackUiState, isSelected: Boolean) = state.copy(isSelected = isSelected)

    fun deletePlaylist(onGotoPlaylistClick: () -> Unit) =
        managers.playlist.deletePlaylist(playlistId = _playlistId.value, onGotoPlaylistClick = onGotoPlaylistClick)

    fun getTrackDownloadUiStateFlow(trackId: String) =
        managers.library.getTrackDownloadUiStateFlow(trackId).stateWhileSubscribed()

    fun onMoveTrack(from: Int, to: Int) {
        _trackUiStates.value = _trackUiStates.value.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun onMoveTrackFinished(from: Int, to: Int) {
        launchOnIOThread { repos.playlist.movePlaylistTrack(_playlistId.value, from, to) }
    }

    fun playPlaylist() = managers.player.playPlaylist(_playlistId.value)

    fun removeSelectedPlaylistTracks() {
        launchOnIOThread {
            repos.playlist.removePlaylistTracks(_playlistId.value, selectedTrackStateIds.value)
            unselectAllTracks()
        }
    }

    fun renamePlaylist(newName: String) = managers.playlist.renamePlaylist(_playlistId.value, newName)
}
