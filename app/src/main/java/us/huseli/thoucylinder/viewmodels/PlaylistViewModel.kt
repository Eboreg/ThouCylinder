package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import us.huseli.thoucylinder.Constants.NAV_ARG_PLAYLIST
import us.huseli.thoucylinder.dataclasses.PlaylistPojo
import us.huseli.thoucylinder.repositories.Repositories
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repos: Repositories,
) : BaseViewModel(repos) {
    val playlistId: UUID = UUID.fromString(savedStateHandle.get<String>(NAV_ARG_PLAYLIST)!!)
    val playlist: Flow<PlaylistPojo> =
        repos.local.playlists.map { playlists -> playlists.find { it.playlistId == playlistId }!! }
    val tracks = repos.local.pageTracksByPlaylistId(playlistId).flow.cachedIn(viewModelScope)
}