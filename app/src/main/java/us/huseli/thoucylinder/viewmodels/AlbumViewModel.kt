package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.AlbumDownloadTask
import us.huseli.thoucylinder.Constants.NAV_ARG_ALBUM
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val repos: Repositories,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext context: Context,
) : AbstractTrackListViewModel("AlbumViewModel", repos) {
    private val _albumId: UUID = UUID.fromString(savedStateHandle.get<String>(NAV_ARG_ALBUM)!!)
    private val _albumPojo = MutableStateFlow<AlbumWithTracksPojo?>(null)
    private val _albumNotFound = MutableStateFlow(false)

    val albumArt: Flow<ImageBitmap?> = _albumPojo.map { it?.getFullImage(context) }.distinctUntilChanged()
    val albumDownloadTask: Flow<AlbumDownloadTask?> = repos.youtube.albumDownloadTasks
        .map { tasks -> tasks.find { it.album.albumId == _albumId } }
        .distinctUntilChanged()
    val albumPojo = _albumPojo.asStateFlow()
    override val trackDownloadTasks = repos.download.tasks
        .map { tasks -> tasks.filter { it.track.albumId == _albumId } }
        .distinctUntilChanged()
    val albumNotFound = _albumNotFound.asStateFlow()
    val trackPojos = repos.track.flowTrackPojosByAlbumId(_albumId)

    init {
        unselectAllTrackPojos()

        viewModelScope.launch(Dispatchers.IO) {
            repos.album.flowAlbumWithTracks(_albumId).distinctUntilChanged().collect { pojo ->
                if (pojo != null) {
                    _albumNotFound.value = false
                    _albumPojo.value = pojo
                } else {
                    _albumNotFound.value = true
                }
            }
        }
    }

    fun loadTrackMetadata(track: Track) {
        /**
         * On-demand fetch (and save, if necessary) of track metadata, because
         * we only want to load it when it is actually going to be used.
         */
        viewModelScope.launch(Dispatchers.IO) {
            ensureTrackMetadata(track)
        }
    }
}
