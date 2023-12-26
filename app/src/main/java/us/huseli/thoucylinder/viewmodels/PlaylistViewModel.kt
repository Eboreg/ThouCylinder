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
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistPojo
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistTrackPojo
import us.huseli.thoucylinder.Repositories
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repos: Repositories,
) : AbstractTrackListViewModel("PlaylistViewModel", repos) {
    private val _selectedTrackPojos = MutableStateFlow<List<PlaylistTrackPojo>>(emptyList())

    val playlistId: UUID = UUID.fromString(savedStateHandle.get<String>(NAV_ARG_PLAYLIST)!!)
    val playlist: Flow<PlaylistPojo?> =
        repos.playlist.playlists.map { playlists -> playlists.find { it.playlistId == playlistId } }
    val selectedPlaylistTrackPojos = _selectedTrackPojos.asStateFlow()
    val trackPojos: Flow<PagingData<PlaylistTrackPojo>> =
        repos.playlist.pageTrackPojosByPlaylistId(playlistId).flow.cachedIn(viewModelScope)

    fun playPlaylist(startAt: PlaylistTrackPojo? = null) = playPlaylist(playlistId, startAt?.track?.trackId)

    fun removeTrackPojos(pojos: List<PlaylistTrackPojo>) = viewModelScope.launch {
        repos.playlist.removePlaylistTracks(pojos)
        _selectedTrackPojos.value -= pojos
    }

    fun selectPlaylistTrackPojos(pojos: List<PlaylistTrackPojo>) {
        val currentIds = _selectedTrackPojos.value.map { pojo -> pojo.track.trackId }
        _selectedTrackPojos.value += pojos.filter { pojo -> !currentIds.contains(pojo.track.trackId) }
    }

    fun toggleSelected(pojo: PlaylistTrackPojo) {
        if (_selectedTrackPojos.value.contains(pojo))
            _selectedTrackPojos.value -= pojo
        else
            _selectedTrackPojos.value += pojo
    }

    override fun unselectAllTrackPojos() {
        _selectedTrackPojos.value = emptyList()
    }
}
