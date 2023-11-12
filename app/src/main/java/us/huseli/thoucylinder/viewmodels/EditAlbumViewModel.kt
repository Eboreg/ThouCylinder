package us.huseli.thoucylinder.viewmodels

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.dataclasses.DiscogsMasterData
import us.huseli.thoucylinder.dataclasses.DiscogsSearchResultItem
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.dataclasses.entities.Style
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.repositories.Repositories
import us.huseli.retaintheme.toBitmap
import javax.inject.Inject

@HiltViewModel
class EditAlbumViewModel @Inject constructor(private val repos: Repositories) : ViewModel() {
    private val _albumPojo = MutableStateFlow<AlbumWithTracksPojo?>(null)
    private val _bitmaps = MutableStateFlow<List<Bitmap>>(emptyList())
    private val _initialTracks = MutableStateFlow<List<Track>>(emptyList())
    private val _loading = MutableStateFlow(false)
    private val _loadingSearchResults = MutableStateFlow(true)
    private val _masters = MutableStateFlow<Map<Int, DiscogsMasterData>>(emptyMap())
    private val _searchResults = MutableStateFlow<List<DiscogsSearchResultItem>>(emptyList())
    private val _selectedMasterId = MutableStateFlow<Int?>(null)

    val albumPojo = _albumPojo.asStateFlow()
    val bitmaps = _bitmaps.asStateFlow()
    val initialTracks = _initialTracks.asStateFlow()
    val loading = _loading.asStateFlow()
    val loadingSearchResults = _loadingSearchResults.asStateFlow()
    val searchResults = _searchResults.asStateFlow()
    val selectedMasterId = _selectedMasterId.asStateFlow()

    fun loadSearchResults(album: Album) = viewModelScope.launch {
        _loadingSearchResults.value = true
        _searchResults.value =
            repos.discogs.searchMasters(query = album.title, artist = album.artist)?.data?.results ?: emptyList()
        _loadingSearchResults.value = false
    }

    fun selectMasterId(masterId: Int, context: Context) = viewModelScope.launch {
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
                        year = master.year?.takeIf { it > 1000 } ?: pojo.album.year,
                    ),
                    genres = master.genres.map { Genre(genreName = it) },
                    styles = master.styles.map { Style(styleName = it) },
                )
            }
            updateTracksFromMaster(master)
            getImages(master, context)
        }
    }

    fun setAlbum(album: Album, context: Context) = viewModelScope.launch {
        repos.room.getAlbumWithTracks(album.albumId)?.also { pojo -> setAlbum(pojo, context) }
    }

    fun setAlbum(pojo: AlbumWithTracksPojo, context: Context) {
        _albumPojo.value = pojo
        _initialTracks.value = pojo.tracks.map { track ->
            track.copy(artist = track.artist ?: pojo.album.artist)
        }
        viewModelScope.launch {
            val bitmap = pojo.album.getFullImage(context)
            if (bitmap != null) _bitmaps.value = listOf(bitmap)
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

    fun unsetAlbum() {
        _albumPojo.value = null
        _initialTracks.value = emptyList()
    }

    fun updateTrack(index: Int, track: Track) {
        _albumPojo.value = _albumPojo.value?.let { pojo ->
            val tracks = pojo.tracks.toMutableList().apply { this[index] = track }
            pojo.copy(tracks = tracks)
        }
    }

    private fun getImages(master: DiscogsMasterData, context: Context) = viewModelScope.launch {
        val bitmaps = mutableListOf<Bitmap>()

        _albumPojo.value?.also { pojo ->
            pojo.album.getFullImage(context)?.also { bitmaps.add(it) }

            repos.mediaStore.collectAlbumImages(pojo).forEach { file ->
                file.toBitmap()?.also { bitmaps.add(it) }
            }

            master.images.filter { it.type == "primary" }.forEach { masterImage ->
                Request(masterImage.uri).getBitmap()?.also { bitmaps.add(it) }
            }
        }

        _bitmaps.value = bitmaps
    }

    private fun updateTracksFromMaster(master: DiscogsMasterData) {
        _albumPojo.value = _albumPojo.value?.let { pojo ->
            val tracks = pojo.tracks.toMutableList().apply {
                val masterPositionPairs = master.getPositionPairs()
                val pojoPositionPairs = pojo.getPositionPairs()

                masterPositionPairs.zip(master.tracklist).forEach { (positions, masterTrack) ->
                    val trackIdx = pojoPositionPairs.indexOfFirst { it == positions }

                    if (trackIdx > -1) {
                        set(
                            trackIdx,
                            this[trackIdx].copy(
                                title = masterTrack.title,
                                artist = masterTrack.artist ?: master.artist,
                                year = masterTrack.year?.takeIf { it > 1000 }
                                    ?: this[trackIdx].year
                                    ?: master.year?.takeIf { it > 1000 }
                                    ?: _albumPojo.value?.album?.year,
                            )
                        )
                    }
                }

                // If our tracklist contains tracks not in master tracklist, reset the "extra" tracks to initial state:
                pojoPositionPairs.forEachIndexed { index, positionPair ->
                    if (!masterPositionPairs.contains(positionPair)) {
                        this[index] = _initialTracks.value[index]
                    }
                }
            }
            pojo.copy(tracks = tracks)
        }
    }
}
