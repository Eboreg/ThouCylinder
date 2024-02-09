package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.Constants.NAV_ARG_PLAYLIST
import us.huseli.thoucylinder.dataclasses.combos.PlaylistPojo
import us.huseli.thoucylinder.dataclasses.combos.PlaylistTrackCombo
import us.huseli.thoucylinder.Repositories
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repos: Repositories,
) : AbstractTrackListViewModel("PlaylistViewModel", repos) {
    private val _selectedTrackCombos = MutableStateFlow<List<PlaylistTrackCombo>>(emptyList())

    val playlistId: UUID = UUID.fromString(savedStateHandle.get<String>(NAV_ARG_PLAYLIST)!!)
    val playlist: Flow<PlaylistPojo?> =
        repos.playlist.playlists.map { playlists -> playlists.find { it.playlistId == playlistId } }
    val selectedPlaylistTrackCombos = _selectedTrackCombos.asStateFlow()
    val trackCombos: Flow<PagingData<PlaylistTrackCombo>> =
        repos.playlist.pageTrackCombosByPlaylistId(playlistId).flow.cachedIn(viewModelScope)

    fun playPlaylist(startAt: PlaylistTrackCombo? = null) = playPlaylist(playlistId, startAt?.track?.trackId)

    fun removeTrackCombos(combos: List<PlaylistTrackCombo>) = viewModelScope.launch {
        repos.playlist.removePlaylistTracks(combos)
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
