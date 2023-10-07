package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import us.huseli.thoucylinder.Constants.NAV_ARG_PLAYLIST
import us.huseli.thoucylinder.dataclasses.PlaylistPojo
import us.huseli.thoucylinder.repositories.LocalRepository
import us.huseli.thoucylinder.repositories.MediaStoreRepository
import us.huseli.thoucylinder.repositories.PlayerRepository
import us.huseli.thoucylinder.repositories.YoutubeRepository
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repo: LocalRepository,
    playerRepo: PlayerRepository,
    youtubeRepo: YoutubeRepository,
    mediaStoreRepo: MediaStoreRepository,
) : BaseViewModel(repo, playerRepo, youtubeRepo, mediaStoreRepo) {
    val playlistId: UUID = UUID.fromString(savedStateHandle.get<String>(NAV_ARG_PLAYLIST)!!)
    val playlist: Flow<PlaylistPojo> =
        repo.playlists.map { playlists -> playlists.find { it.playlistId == playlistId }!! }
    val tracks = repo.pageTracksByPlaylistId(playlistId).flow.cachedIn(viewModelScope)
}
