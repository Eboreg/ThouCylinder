package us.huseli.thoucylinder.viewmodels

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.dataclasses.Album
import us.huseli.thoucylinder.repositories.LocalRepository
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(private val repo: LocalRepository) : ViewModel() {
    private val _fetchedAlbumArt = mutableListOf<UUID>()
    private val _albumArt = MutableStateFlow<Map<UUID, ImageBitmap>>(emptyMap())

    val albums = repo.libraryAlbums
    val singleTracks = repo.singleTracks

    fun deleteAll() = viewModelScope.launch { repo.deleteAll() }

    fun getAlbumArt(album: Album) = MutableStateFlow<ImageBitmap?>(null).apply {
        val albumArt = _albumArt.value[album.albumId]

        if (albumArt != null) value = albumArt
        else viewModelScope.launch {
            if (!_fetchedAlbumArt.contains(album.albumId)) {
                _fetchedAlbumArt.add(album.albumId)
                album.albumArt?.getImageBitmap()?.also {
                    value = it
                    _albumArt.value += album.albumId to it
                }
            }
        }
    }.asStateFlow()
}
