package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.dataclasses.AlbumPojo
import us.huseli.thoucylinder.dataclasses.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.Track
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
    val albums = youtubeRepo.albumSearchResults.map { albums -> albums.map { AlbumPojo(album = it) } }
    val tracks = youtubeRepo.trackSearchResults

    suspend fun loadTrackMetadata(track: Track) {
        var metadata = track.metadata
        var youtubeMetadata = track.youtubeVideo?.metadata

        if (track.youtubeVideo != null) {
            if (youtubeMetadata == null) youtubeMetadata = youtubeRepo.getBestMetadata(track.youtubeVideo.id)
            if (metadata == null) metadata = youtubeMetadata?.toTrackMetadata()
            youtubeRepo.updateSearchResultTrack(
                track.copy(metadata = metadata, youtubeVideo = track.youtubeVideo.copy(metadata = youtubeMetadata))
            )
        }
    }

    fun populateTempAlbum(pojo: AlbumPojo) {
        repo.addOrUpdateTempAlbum(AlbumWithTracksPojo(album = pojo.album, tracks = emptyList()))
        viewModelScope.launch(Dispatchers.IO) {
            pojo.album.youtubePlaylist?.let { playlist ->
                val videos = youtubeRepo.listPlaylistVideos(playlist = playlist, withMetadata = false)
                val tracks = videos.map { video ->
                    video.toTrack(
                        isInLibrary = false,
                        albumId = pojo.album.albumId,
                        image = video.thumbnail ?: pojo.album.albumArt,
                    )
                }

                repo.addOrUpdateTempAlbum(AlbumWithTracksPojo(album = pojo.album, tracks = tracks))
            }
        }
    }

    fun search(query: String) = viewModelScope.launch(Dispatchers.IO) {
        _isSearching.value = true
        youtubeRepo.search(query)
        _isSearching.value = false
    }
}
