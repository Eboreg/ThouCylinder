package us.huseli.thoucylinder.viewmodels

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.Constants.NAV_ARG_ALBUM
import us.huseli.thoucylinder.LoadStatus
import us.huseli.thoucylinder.dataclasses.Album
import us.huseli.thoucylinder.repositories.LocalRepository
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val repo: LocalRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _albumId: UUID = UUID.fromString(savedStateHandle.get<String>(NAV_ARG_ALBUM)!!)
    private val _album = MutableStateFlow<Album?>(null)
    private val _albumArt = MutableStateFlow<ImageBitmap?>(null)
    private val _albumArtLoadStatus = MutableStateFlow(LoadStatus.LOADING)

    val album = _album.asStateFlow()
    val albumArt = _albumArt.asStateFlow()
    val albumArtLoadStatus = _albumArtLoadStatus.asStateFlow()

    init {
        viewModelScope.launch {
            repo.albums.mapNotNull { albums -> albums.find { it.id == _albumId } }
                .distinctUntilChanged()
                .collect { album ->
                    _album.value = album

                    _albumArt.value = album.albumArt?.getImageBitmap()
                    _albumArtLoadStatus.value = LoadStatus.LOADED
                }
        }
    }
}