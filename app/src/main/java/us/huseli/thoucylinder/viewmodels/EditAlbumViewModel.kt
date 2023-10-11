package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.DiscogsMasterData
import us.huseli.thoucylinder.dataclasses.DiscogsSearchResultItem
import us.huseli.thoucylinder.dataclasses.Image
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.dataclasses.entities.Style
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.repositories.Repositories
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditAlbumViewModel @Inject constructor(private val repos: Repositories) : ViewModel() {
    private val _albumPojo = MutableStateFlow<AlbumWithTracksPojo?>(null)
    private val _images = MutableStateFlow<List<Pair<Image, ImageBitmap>>>(emptyList())
    private val _initialTracks = MutableStateFlow<List<Track>>(emptyList())
    private val _loading = MutableStateFlow(false)
    private val _loadingSearchResults = MutableStateFlow(true)
    private val _masters = MutableStateFlow<Map<Int, DiscogsMasterData>>(emptyMap())
    private val _searchResults = MutableStateFlow<List<DiscogsSearchResultItem>>(emptyList())
    private val _selectedMasterId = MutableStateFlow<Int?>(null)

    val albumPojo = _albumPojo.asStateFlow()
    val images = _images.asStateFlow()
    val initialTracks = _initialTracks.asStateFlow()
    val loading = _loading.asStateFlow()
    val loadingSearchResults = _loadingSearchResults.asStateFlow()
    val searchResults = _searchResults.asStateFlow()
    val selectedMasterId = _selectedMasterId.asStateFlow()

    fun loadSearchResults(album: Album) = viewModelScope.launch(Dispatchers.IO) {
        _loadingSearchResults.value = true
        _searchResults.value =
            repos.discogs.searchMasters(query = album.title, artist = album.artist)?.data?.results ?: emptyList()
        _loadingSearchResults.value = false
    }

    fun selectMasterId(masterId: Int, context: Context) = viewModelScope.launch(Dispatchers.IO) {
        val master =
            _masters.value[masterId]
                ?: repos.discogs.getMaster(masterId)?.data?.also { _masters.value += masterId to it }

        if (master != null) {
            _selectedMasterId.value = masterId
            _albumPojo.value?.also { pojo ->
                _albumPojo.value = pojo.copy(
                    album = pojo.album.copy(
                        title = master.title,
                        artist = master.artist,
                        year = master.year,
                    ),
                    genres = master.genres.map { Genre(genreName = it) },
                    styles = master.styles.map { Style(styleName = it) },
                )
            }
            updateTracksFromMaster(master)
            getImages(master, context)
        }
    }

    fun setAlbum(pojo: AlbumWithTracksPojo) {
        _albumPojo.value = pojo
        _initialTracks.value = pojo.tracks.map { track ->
            track.copy(artist = track.artist ?: pojo.album.artist)
        }
        if (pojo.album.albumArt != null) {
            viewModelScope.launch(Dispatchers.IO) {
                val imageBitmap = pojo.album.albumArt.getImageBitmap()
                if (imageBitmap != null) _images.value = listOf(Pair(pojo.album.albumArt, imageBitmap))
            }
        }
    }

    fun setArtist(value: String) {
        _albumPojo.value = _albumPojo.value?.let { pojo ->
            pojo.copy(album = pojo.album.copy(artist = value))
        }
    }

    fun setTitle(value: String) {
        _albumPojo.value = _albumPojo.value?.let { pojo ->
            pojo.copy(album = pojo.album.copy(title = value))
        }
    }

    private fun getImages(master: DiscogsMasterData, context: Context) = viewModelScope.launch(Dispatchers.IO) {
        val pairs = mutableListOf<Pair<Image, ImageBitmap>>()

        _albumPojo.value?.album?.albumArt?.also { image ->
            image.getImageBitmap()?.also { pairs.add(Pair(image, it)) }
        }
        _albumPojo.value?.also { pojo ->
            repos.mediaStore.getAlbumArtFromAlbumFolder(pojo).forEach { importedImage ->
                val image = Image(
                    width = importedImage.bitmap.width,
                    height = importedImage.bitmap.height,
                    localFile = importedImage.file,
                )
                pairs.add(Pair(image, importedImage.bitmap.asImageBitmap()))
            }
        }
        master.images.filter { it.type == "primary" }.forEach { masterImage ->
            val image = Image(
                width = masterImage.width,
                height = masterImage.height,
                localFile = File(context.cacheDir, UUID.randomUUID().toString()),
                url = masterImage.uri,
            )
            image.getImageBitmap()?.also { pairs.add(Pair(image, it)) }
        }
        _images.value = pairs
    }

    fun updateTrack(index: Int, track: Track) {
        _albumPojo.value = _albumPojo.value?.let { pojo ->
            val tracks = pojo.tracks.toMutableList().apply { this[index] = track }
            pojo.copy(tracks = tracks)
        }
    }

    private fun updateTracksFromMaster(master: DiscogsMasterData) {
        _albumPojo.value = _albumPojo.value?.let { pojo ->
            val tracks = pojo.tracks.toMutableList().apply {
                master.tracklist.forEachIndexed { masterTrackIdx, masterTrack ->
                    indexOfFirst { it.albumPosition == masterTrackIdx + 1 }.takeIf { it > -1 }?.let { trackIdx ->
                        set(
                            trackIdx,
                            this[trackIdx].copy(
                                title = masterTrack.title,
                                artist = masterTrack.artist ?: master.artist,
                                year = masterTrack.year ?: this[trackIdx].year ?: master.year,
                            )
                        )
                    }
                }
                // If master tracklist is shorter than our tracklist, reset the "extra" tracks to initial state:
                forEachIndexed { trackIdx, track ->
                    if (track.albumPosition != null && track.albumPosition > master.tracklist.size) {
                        this[trackIdx] = _initialTracks.value[trackIdx]
                    }
                }
            }
            pojo.copy(tracks = tracks)
        }
    }
}
