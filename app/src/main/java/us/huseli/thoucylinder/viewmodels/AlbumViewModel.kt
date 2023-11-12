package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.Constants.NAV_ARG_ALBUM
import us.huseli.thoucylinder.dataclasses.DownloadProgress
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.repositories.Repositories
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val repos: Repositories,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext context: Context,
) : AbstractSelectViewModel("AlbumViewModel", repos) {
    private val _albumId: UUID = UUID.fromString(savedStateHandle.get<String>(NAV_ARG_ALBUM)!!)
    private val _albumPojo = MutableStateFlow<AlbumWithTracksPojo?>(null)

    val albumArt: Flow<ImageBitmap?> = _albumPojo.map { it?.album?.getFullImage(context)?.asImageBitmap() }
    val albumPojo = _albumPojo.asStateFlow()
    val downloadProgress: Flow<DownloadProgress?> =
        repos.youtube.albumDownloadProgressMap.map { it[_albumId] }.distinctUntilChanged()

    init {
        viewModelScope.launch {
            repos.room.flowAlbumWithTracks(_albumId).filterNotNull().distinctUntilChanged().collect { pojo ->
                _albumPojo.value = pojo
            }
        }
    }

    fun enqueueAlbum(context: Context) {
        _albumPojo.value?.also { pojo -> enqueueTrackPojos(pojo.trackPojos, context) }
    }

    fun loadTrackMetadata(track: Track) {
        /**
         * On-demand fetch of track metadata, because we only want to load it
         * when it is actually going to be used. Does not save anything to DB
         * yet.
         */
        if (track.metadata == null) viewModelScope.launch {
            _albumPojo.value = _albumPojo.value?.let { pojo ->
                val tracks = pojo.tracks.map { if (it.trackId == track.trackId) ensureTrackMetadata(track) else it }
                pojo.copy(tracks = tracks)
            }
        }
    }

    fun playAlbum(startAt: Track? = null) {
        _albumPojo.value?.also { pojo ->
            playTrackPojos(pojo.trackPojos, startAt?.let { pojo.indexOfTrack(it) })
        }
    }
}
