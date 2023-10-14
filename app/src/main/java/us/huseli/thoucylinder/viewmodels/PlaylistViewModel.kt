package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.Constants.NAV_ARG_PLAYLIST
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistPojo
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import us.huseli.thoucylinder.repositories.Repositories
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repos: Repositories,
) : BaseViewModel(repos) {
    val playlistId: UUID = UUID.fromString(savedStateHandle.get<String>(NAV_ARG_PLAYLIST)!!)
    val playlist: Flow<PlaylistPojo> =
        repos.local.playlists.map { playlists -> playlists.find { it.playlistId == playlistId }!! }
    val tracks: Flow<PagingData<TrackPojo>> =
        repos.local.pageTracksByPlaylistId(playlistId).flow.cachedIn(viewModelScope)

    fun playPlaylist(startAt: Track) = viewModelScope.launch {
        val tracks = repos.local.listPlaylistTracks(playlistId)
        repos.player.replaceAndPlay(tracks = tracks, startIndex = tracks.map { it.trackId }.indexOf(startAt.trackId))
    }
}
