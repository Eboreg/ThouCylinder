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
class LibraryViewModel @Inject constructor(
    repo: LocalRepository,
) : ViewModel() {
    private val _fetchedAlbumArt = mutableListOf<UUID>()
    private val _albumArt = MutableStateFlow<Map<UUID, ImageBitmap>>(emptyMap())

    val albums = repo.albums
    val singleTracks = repo.singleTracks

    fun getAlbumArt(album: Album) = MutableStateFlow<ImageBitmap?>(null).apply {
        val albumArt = _albumArt.value[album.id]

        if (albumArt != null) value = albumArt
        else viewModelScope.launch {
            if (!_fetchedAlbumArt.contains(album.id)) {
                _fetchedAlbumArt.add(album.id)
                album.albumArt?.getImageBitmap()?.also {
                    value = it
                    _albumArt.value += album.id to it
                }
            }
        }
    }.asStateFlow()
}
