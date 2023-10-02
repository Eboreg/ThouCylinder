package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.dataclasses.Image
import us.huseli.thoucylinder.dataclasses.Track
import us.huseli.thoucylinder.repositories.LocalRepository
import us.huseli.thoucylinder.repositories.PlayerRepository
import us.huseli.thoucylinder.repositories.YoutubeRepository
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repo: LocalRepository,
    playerRepo: PlayerRepository,
    youtubeRepo: YoutubeRepository,
) : BaseViewModel(playerRepo, repo, youtubeRepo) {
    private val _artistImages = MutableStateFlow<Map<String, Image>>(emptyMap())
    private val _displayType = MutableStateFlow(DisplayType.LIST)
    private val _listType = MutableStateFlow(ListType.ALBUMS)

    val albums = repo.libraryAlbums
    val artistImages = _artistImages.asStateFlow()
    val artistsWithTracks: Flow<Map<String, List<Track>>> = repo.tracks
        .map { tracks -> tracks.groupBy { it.artist ?: "Unknown artist" }.toSortedMap() }
        .distinctUntilChanged()
    val tracks = repo.tracks
    val displayType = _displayType.asStateFlow()
    val listType = _listType.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) { _artistImages.value = repo.collectArtistImages() }
        viewModelScope.launch(Dispatchers.IO) { repo.deleteOrphanTracksAndAlbums() }
        viewModelScope.launch(Dispatchers.IO) { repo.importNewMediaStoreAlbums() }
    }

    fun deleteAll() = viewModelScope.launch { repo.deleteAll() }

    fun setDisplayType(value: DisplayType) {
        _displayType.value = value
    }

    fun setListType(value: ListType) {
        _listType.value = value
    }
}
