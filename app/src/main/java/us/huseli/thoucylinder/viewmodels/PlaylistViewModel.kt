package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.Constants.NAV_ARG_PLAYLIST
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.combos.PlaylistTrackCombo
import us.huseli.thoucylinder.dataclasses.combos.toPlaylistTracks
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repos: Repositories,
) : AbstractTrackListViewModel("PlaylistViewModel", repos) {
    private val _selectedTrackCombos = MutableStateFlow<List<PlaylistTrackCombo>>(emptyList())
    private val _trackCombos = MutableStateFlow<List<PlaylistTrackCombo>>(emptyList())

    val playlistId: UUID = UUID.fromString(savedStateHandle.get<String>(NAV_ARG_PLAYLIST)!!)
    val playlist = repos.playlist.flowPlaylist(playlistId)
    val selectedPlaylistTrackCombos = _selectedTrackCombos.asStateFlow()
    val trackCombos = _trackCombos.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repos.playlist.flowPlaylistTracks(playlistId).collect { combos ->
                _trackCombos.value = combos
            }
        }
    }

    fun onMoveTrack(from: Int, to: Int) {
        _trackCombos.value = _trackCombos.value.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun onMoveTrackFinished(from: Int, to: Int) = viewModelScope.launch(Dispatchers.IO) {
        repos.playlist.movePlaylistTrack(playlistId, from, to)
    }

    fun playPlaylist(startAt: PlaylistTrackCombo? = null) = playPlaylist(playlistId, startAt?.track?.trackId)

    fun removeTrackCombos(combos: List<PlaylistTrackCombo>) = viewModelScope.launch {
        repos.playlist.removePlaylistTracks(combos.toPlaylistTracks())
        _selectedTrackCombos.value -= combos
    }

    fun selectPlaylistTrackCombos(combos: List<PlaylistTrackCombo>) {
        val currentIds = _selectedTrackCombos.value.map { combo -> combo.track.trackId }
        _selectedTrackCombos.value += combos.filter { combo -> !currentIds.contains(combo.track.trackId) }
    }

    fun toggleSelected(combo: PlaylistTrackCombo) {
        if (_selectedTrackCombos.value.contains(combo))
            _selectedTrackCombos.value -= combo
        else
            _selectedTrackCombos.value += combo
    }

    override fun unselectAllTrackCombos() {
        _selectedTrackCombos.value = emptyList()
    }
}
