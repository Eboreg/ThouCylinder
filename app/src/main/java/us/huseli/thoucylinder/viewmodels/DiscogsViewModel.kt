package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.dataclasses.Album
import us.huseli.thoucylinder.dataclasses.DiscogsMasterData
import us.huseli.thoucylinder.dataclasses.DiscogsSearchResultItem
import us.huseli.thoucylinder.dataclasses.Image
import us.huseli.thoucylinder.dataclasses.Track
import us.huseli.thoucylinder.repositories.DiscogsRepository
import us.huseli.thoucylinder.thumbnailDir
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DiscogsViewModel @Inject constructor(private val repo: DiscogsRepository) : ViewModel() {
    private val _album = MutableStateFlow<Album?>(null)
    private val _images = MutableStateFlow<List<Pair<Image, ImageBitmap>>>(emptyList())
    private val _initialTracks = MutableStateFlow<List<Track>>(emptyList())
    private val _loading = MutableStateFlow(false)
    private val _loadingSearchResults = MutableStateFlow(true)
    private val _masters = MutableStateFlow<Map<Int, DiscogsMasterData>>(emptyMap())
    private val _searchResults = MutableStateFlow<List<DiscogsSearchResultItem>>(emptyList())
    private val _selectedMasterId = MutableStateFlow<Int?>(null)

    val album = _album.asStateFlow()
    val images = _images.asStateFlow()
    val initialTracks = _initialTracks.asStateFlow()
    val loading = _loading.asStateFlow()
    val loadingSearchResults = _loadingSearchResults.asStateFlow()
    val searchResults = _searchResults.asStateFlow()
    val selectedMasterId = _selectedMasterId.asStateFlow()

    fun selectMasterId(masterId: Int, context: Context) = viewModelScope.launch(Dispatchers.IO) {
        val master =
            _masters.value[masterId] ?: repo.getMaster(masterId)?.data?.also { _masters.value += masterId to it }

        if (master != null) {
            _selectedMasterId.value = masterId
            _album.value = _album.value?.copy(
                title = master.title,
                artist = master.artist,
                year = master.year,
                genres = master.genres,
                styles = master.styles,
            )
            updateTracksFromMaster(master)
            updateImages(master, context)
        }
    }

    fun setAlbum(album: Album) {
        _album.value = album
        _initialTracks.value = album.tracks
        loadSearchResults(album)
        viewModelScope.launch(Dispatchers.IO) {
            val imageBitmap = album.albumArt?.getImageBitmap()
            if (album.albumArt != null && imageBitmap != null)
                _images.value = listOf(Pair(album.albumArt, imageBitmap))
        }
    }

    fun setArtist(value: String) {
        _album.value = _album.value?.copy(artist = value)
    }

    fun setTitle(value: String) {
        _album.value = _album.value?.copy(title = value)
    }

    fun updateTrack(index: Int, track: Track) {
        _album.value = _album.value?.let { album ->
            val tracks = album.tracks.toMutableList().apply { this[index] = track }
            album.copy(tracks = tracks)
        }
    }

    fun loadSearchResults(album: Album) = viewModelScope.launch(Dispatchers.IO) {
        _loadingSearchResults.value = true
        _searchResults.value =
            repo.searchMasters(query = album.title, artist = album.artist)?.data?.results ?: emptyList()
        _loadingSearchResults.value = false
    }

    private fun updateImages(master: DiscogsMasterData, context: Context) = viewModelScope.launch(Dispatchers.IO) {
        val pairs = mutableListOf<Pair<Image, ImageBitmap>>()

        _album.value?.albumArt?.let { image ->
            image.getImageBitmap()?.let { pairs.add(Pair(image, it)) }
        }
        master.images.filter { it.type == "primary" }.forEach { masterImage ->
            val image = Image(
                width = masterImage.width,
                height = masterImage.height,
                localFile = File(context.thumbnailDir, UUID.randomUUID().toString()),
                url = masterImage.uri,
            )
            image.getImageBitmap()?.let { pairs.add(Pair(image, it)) }
        }
        _images.value = pairs
    }

    private fun updateTracksFromMaster(master: DiscogsMasterData) {
        _album.value = _album.value?.let { album ->
            val tracks = album.tracks.toMutableList().apply {
                master.tracklist.forEachIndexed { masterTrackIdx, masterTrack ->
                    indexOfFirst { it.albumPosition == masterTrackIdx + 1 }.takeIf { it > -1 }?.let { trackIdx ->
                        set(
                            trackIdx,
                            this[trackIdx].copy(
                                title = masterTrack.title,
                                artist = masterTrack.artist,
                                year = masterTrack.year,
                            )
                        )
                    }
                }
            }
            album.copy(tracks = tracks)
        }
    }
}
