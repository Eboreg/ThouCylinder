package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.pojos.ArtistPojo
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import us.huseli.thoucylinder.repositories.Repositories
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repos: Repositories,
) : AbstractSelectViewModel("LibraryViewModel", repos) {
    private val _artistImages = MutableStateFlow<Map<String, File>>(emptyMap())
    private val _displayType = MutableStateFlow(DisplayType.LIST)
    private val _listType = MutableStateFlow(ListType.ALBUMS)

    val albumPojos = repos.room.albumPojos
    val artistImages = _artistImages.asStateFlow()
    val artistPojos: Flow<List<ArtistPojo>> = repos.room.artistPojos
    val displayType = _displayType.asStateFlow()
    val listType = _listType.asStateFlow()
    val pagingTrackPojos: Flow<PagingData<TrackPojo>> = repos.room.trackPojoPager.flow.cachedIn(viewModelScope)
    val playlists = repos.room.playlists

    init {
        viewModelScope.launch { _artistImages.value = repos.mediaStore.collectArtistImages() }
    }

    fun deleteAll() = viewModelScope.launch { repos.room.deleteAll() }

    suspend fun getPlaylistImage(playlistId: UUID, context: Context): ImageBitmap? =
        repos.room.listPlaylistAlbums(playlistId).firstNotNullOfOrNull { album ->
            album.getThumbnail(context)?.asImageBitmap()
        }

    fun selectAlbumsFromLastSelected(to: Album) = viewModelScope.launch {
        selectAlbumsFromLastSelected(albums = albumPojos.last().map { it.album }, to = to)
    }

    fun setDisplayType(value: DisplayType) {
        _displayType.value = value
    }

    fun setListType(value: ListType) {
        _listType.value = value
    }
}
