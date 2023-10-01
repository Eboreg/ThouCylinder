package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.dataclasses.Album
import us.huseli.thoucylinder.repositories.LocalRepository
import us.huseli.thoucylinder.repositories.PlayerRepository
import us.huseli.thoucylinder.repositories.YoutubeRepository
import javax.inject.Inject

@HiltViewModel
class YoutubeSearchViewModel @Inject constructor(
    private val youtubeRepo: YoutubeRepository,
    playerRepo: PlayerRepository,
    private val repo: LocalRepository,
) : BaseViewModel(playerRepo, repo, youtubeRepo) {
    private val _isSearching = MutableStateFlow(false)

    val isSearching = _isSearching.asStateFlow()
    val albums = youtubeRepo.albumSearchResults
    val tracks = youtubeRepo.trackSearchResults

    fun populateTempAlbum(album: Album) {
        repo.addOrUpdateTempAlbum(album)
        viewModelScope.launch(Dispatchers.IO) {
            album.youtubePlaylist?.let { playlist ->
                val videos = youtubeRepo.listPlaylistVideos(playlist = playlist, withMetadata = false)
                val tracks = videos.map { video ->
                    video.toTrack(
                        isInLibrary = false,
                        albumId = album.albumId,
                        image = video.thumbnail ?: album.albumArt,
                    )
                }

                repo.addOrUpdateTempAlbum(album.copy(tracks = tracks))
            }
        }
    }

    fun search(query: String) = viewModelScope.launch(Dispatchers.IO) {
        _isSearching.value = true
        youtubeRepo.search(query)
        _isSearching.value = false
    }
}
