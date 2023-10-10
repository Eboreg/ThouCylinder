package us.huseli.thoucylinder.viewmodels

import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.Constants.NAV_ARG_ALBUM
import us.huseli.thoucylinder.LoadStatus
import us.huseli.thoucylinder.dataclasses.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.DownloadProgress
import us.huseli.thoucylinder.dataclasses.TrackMetadata
import us.huseli.thoucylinder.dataclasses.YoutubeMetadata
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.repositories.Repositories
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val repos: Repositories,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel(repos) {
    private val _albumId: UUID = UUID.fromString(savedStateHandle.get<String>(NAV_ARG_ALBUM)!!)

    private val _albumArt = MutableStateFlow<ImageBitmap?>(null)
    private val _albumArtLoadStatus = MutableStateFlow(LoadStatus.LOADING)
    private val _albumPojo = MutableStateFlow<AlbumWithTracksPojo?>(null)
    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    private val _trackDownloadProgress = MutableStateFlow<Map<UUID, DownloadProgress>>(emptyMap())

    val albumArt = _albumArt.asStateFlow()
    val albumArtLoadStatus = _albumArtLoadStatus.asStateFlow()
    val albumPojo = _albumPojo.asStateFlow()
    val downloadProgress = _downloadProgress.asStateFlow()
    val trackDownloadProgress = _trackDownloadProgress.asStateFlow()

    private var downloadJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            combine(repos.local.getAlbumWithTracks(_albumId), repos.local.tempAlbumPojos) { pojo, tempPojos ->
                pojo ?: tempPojos[_albumId]
            }.filterNotNull().distinctUntilChanged().collect { pojo ->
                _albumPojo.value = pojo
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            albumPojo.filterNotNull().distinctUntilChanged().collect { pojo ->
                _albumArt.value = pojo.album.albumArt?.getImageBitmap()
                _albumArtLoadStatus.value = LoadStatus.LOADED
            }
        }
    }

    fun cancelDownload() = downloadJob?.cancel()

    fun deleteAlbumWithTracks() = viewModelScope.launch(Dispatchers.IO) {
        if (BuildConfig.DEBUG) {
            _albumPojo.value?.also { repos.local.deleteAlbumWithTracks(it) }
        }
    }

    fun downloadAndSaveAlbum(pojo: AlbumWithTracksPojo) {
        _albumPojo.value = pojo

        downloadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val tracks = repos.youtube.downloadTracks(
                    tracks = pojo.tracks,
                    progressCallback = { _downloadProgress.value = it },
                )
                val newPojo = repos.mediaStore.moveTaggedAlbumToMediaStore(
                    pojo = pojo.copy(tracks = tracks, album = pojo.album.copy(isLocal = true)),
                    progressCallback = { _downloadProgress.value = it },
                )
                _downloadProgress.value = null
                repos.local.saveAlbumWithTracks(newPojo)
                _albumPojo.value = newPojo
            } catch (e: Exception) {
                Log.e("download", e.toString(), e)
            } finally {
                _downloadProgress.value = null
                downloadJob = null
            }
        }
    }

    /**
     * On-demand fetch of track metadata, because we only want to load it when it is actually going to be used.
     * Does not save anything to DB yet.
     */
    fun loadTrackMetadata(track: Track) {
        if (track.metadata == null) viewModelScope.launch(Dispatchers.IO) {
            _albumPojo.value = _albumPojo.value?.let { pojo ->
                pojo.copy(
                    tracks = pojo.tracks.map { if (it.trackId == track.trackId) ensureTrackMetadata(track) else it },
                )
            }
        }
    }

    fun playAlbum() {
        _albumPojo.value?.also { repos.player.playAlbum(it) }
    }

    fun removeAlbumFromLibrary() = viewModelScope.launch(Dispatchers.IO) {
        _albumPojo.value?.let { repos.local.deleteAlbumWithTracks(it) }
    }

    fun saveAlbumWithTracks(pojo: AlbumWithTracksPojo) {
        _albumPojo.value = pojo
        viewModelScope.launch(Dispatchers.IO) {
            repos.local.saveAlbumWithTracks(ensureTrackMetadata(pojo))
        }
    }

    fun tagAlbumTracks(pojo: AlbumWithTracksPojo) = viewModelScope.launch(Dispatchers.IO) {
        repos.mediaStore.tagAlbumTracks(ensureTrackMetadata(pojo))
    }

    private suspend fun ensureTrackMetadata(pojo: AlbumWithTracksPojo): AlbumWithTracksPojo = pojo.copy(
        tracks = pojo.tracks.map { track -> ensureTrackMetadata(track) }
    )

    private suspend fun ensureTrackMetadata(track: Track): Track {
        val (metadata, youtubeMetadata) = getTrackAndYoutubeMetadata(track)

        return track.copy(
            metadata = metadata,
            youtubeVideo = track.youtubeVideo?.copy(metadata = youtubeMetadata),
        )
    }

    private suspend fun getTrackAndYoutubeMetadata(track: Track): Pair<TrackMetadata?, YoutubeMetadata?> {
        val youtubeMetadata = track.youtubeVideo?.metadata ?: repos.youtube.getBestMetadata(track)
        val metadata = track.metadata ?: youtubeMetadata?.toTrackMetadata()

        return Pair(metadata, youtubeMetadata)
    }
}
