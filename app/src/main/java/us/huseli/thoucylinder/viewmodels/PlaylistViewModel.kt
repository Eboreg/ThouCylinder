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
import us.huseli.thoucylinder.repositories.Repositories
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repos: Repositories,
) : AbstractTrackListViewModel("PlaylistViewModel", repos) {
    private val _selectedPlaylistTracks = MutableStateFlow<List<PlaylistTrackPojo>>(emptyList())

    val playlistId: UUID = UUID.fromString(savedStateHandle.get<String>(NAV_ARG_PLAYLIST)!!)

    val playlist: Flow<PlaylistPojo> =
        repos.room.playlists.map { playlists -> playlists.find { it.playlistId == playlistId }!! }
    val selectedPlaylistTracks = _selectedPlaylistTracks.asStateFlow()
    val trackPojos: Flow<PagingData<PlaylistTrackPojo>> =
        repos.room.pageTrackPojosByPlaylistId(playlistId).flow.cachedIn(viewModelScope)

    fun playPlaylist(startAt: PlaylistTrackPojo? = null) = playPlaylist(playlistId, startAt?.trackId)

    fun removeTrackPojos(pojos: List<PlaylistTrackPojo>) = viewModelScope.launch {
        repos.room.removePlaylistTracks(pojos)
    }

    fun selectTrackPojosFromLastSelected(to: PlaylistTrackPojo) = viewModelScope.launch {
        val lastSelected = _selectedPlaylistTracks.value.lastOrNull()

        if (lastSelected != null)
            selectTrackPojos(repos.room.listPlaylistTracksBetween(playlistId, lastSelected, to))
        else selectTrackPojos(listOf(to))
    }

    fun toggleSelected(pojo: PlaylistTrackPojo) {
        if (_selectedPlaylistTracks.value.contains(pojo))
            _selectedPlaylistTracks.value -= pojo
        else
            _selectedPlaylistTracks.value += pojo
    }

    override fun unselectAllTrackPojos() {
        _selectedPlaylistTracks.value = emptyList()
    }

    private fun selectTrackPojos(tracks: List<PlaylistTrackPojo>) {
        val currentIds = _selectedPlaylistTracks.value.map { track -> track.trackId }
        _selectedPlaylistTracks.value += tracks.filter { track -> !currentIds.contains(track.trackId) }
    }
}
