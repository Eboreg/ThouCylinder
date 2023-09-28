package us.huseli.thoucylinder.viewmodels

import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.Constants.NAV_ARG_ALBUM
import us.huseli.thoucylinder.LoadStatus
import us.huseli.thoucylinder.dataclasses.Album
import us.huseli.thoucylinder.dataclasses.DownloadProgress
import us.huseli.thoucylinder.dataclasses.Track
import us.huseli.thoucylinder.dataclasses.TrackMetadata
import us.huseli.thoucylinder.dataclasses.YoutubeVideo
import us.huseli.thoucylinder.repositories.LocalRepository
import us.huseli.thoucylinder.repositories.PlayerRepository
import us.huseli.thoucylinder.repositories.YoutubeRepository
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val repo: LocalRepository,
    private val playerRepo: PlayerRepository,
    private val youtubeRepo: YoutubeRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _albumId: UUID? = savedStateHandle.get<String>(NAV_ARG_ALBUM)?.let { UUID.fromString(it) }

    private val _album = MutableStateFlow<Album?>(null)
    private val _albumArt = MutableStateFlow<ImageBitmap?>(null)
    private val _albumArtLoadStatus = MutableStateFlow(LoadStatus.LOADING)
    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    private val _downloadedAlbum = MutableStateFlow<Album?>(null)
    private val _trackDownloadProgress = MutableStateFlow<Map<UUID, DownloadProgress>>(emptyMap())
    private val _videos = MutableStateFlow<List<YoutubeVideo>>(emptyList())

    val album = _album.asStateFlow()
    val albumArt = _albumArt.asStateFlow()
    val albumArtLoadStatus = _albumArtLoadStatus.asStateFlow()
    val downloadProgress = _downloadProgress.asStateFlow()
    val downloadedAlbum = _downloadedAlbum.asStateFlow()
    val playingUri = playerRepo.playingUri
    val trackDownloadProgress = _trackDownloadProgress.asStateFlow()

    private var downloadJob: Job? = null

    init {
        _albumId?.let { albumId ->
            viewModelScope.launch {
                combine(repo.libraryAlbums, repo.tempAlbums) { libraryAlbums, tempAlbums ->
                    libraryAlbums.plus(tempAlbums).find { it.albumId == albumId }
                }.filterNotNull().distinctUntilChanged().collect { _album.value = it }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            _album.filterNotNull().distinctUntilChanged().collect { album ->
                _albumArt.value = album.albumArt?.getImageBitmap()
                _albumArtLoadStatus.value = LoadStatus.LOADED
            }
        }
    }

    fun addToLibrary(album: Album) {
        _album.value = album
        viewModelScope.launch {
            repo.addAlbumToLibrary(
                album.copy(
                    tracks = album.tracks.map { track ->
                        track.copy(metadata = getTrackMetadata(track))
                    }
                )
            )
        }
    }

    fun cancelDownload() = downloadJob?.cancel()

    fun deleteDownloadedAlbum() {
        _downloadedAlbum.value?.let { album ->
            viewModelScope.launch(Dispatchers.IO) {
                album.tracks.forEach { track ->
                    track.tempTrackData?.localFile?.delete()
                }
            }
        }
        _downloadedAlbum.value = null
    }

    fun deleteLocalFiles() = viewModelScope.launch {
        _album.value?.let { repo.deleteLocalFiles(it) }
    }

    fun downloadAndAddToLibrary(album: Album) {
        _album.value = album
        downloadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                fetchVideos()
                try {
                    val tracks = youtubeRepo.downloadTracks(
                        videos = _videos.value,
                        progressCallback = { _downloadProgress.value = it },
                    )
                    album.copy(tracks = tracks).also {
                        _album.value = it
                        saveDownloadedAlbum(it)
                    }
                } catch (e: Exception) {
                    Log.e("download", e.toString(), e)
                } finally {
                    _downloadProgress.value = null
                }
            } catch (_: CancellationException) {
                deleteDownloadedAlbum()
            } finally {
                downloadJob = null
            }
        }
    }

    fun downloadTrack(track: Track) = viewModelScope.launch(Dispatchers.IO) {
        track.youtubeVideo?.let { video ->
            try {
                val newTrack = youtubeRepo.downloadTrack(
                    video = video,
                    statusCallback = {
                        _trackDownloadProgress.value += track.id to it.copy(progress = it.progress * 0.8)
                    },
                )
                repo.moveAndInsertTrack(newTrack) {
                    _trackDownloadProgress.value += track.id to it.copy(progress = 0.8 + (it.progress * 0.2))
                }
            } catch (e: Exception) {
                Log.e("downloadTrack", e.toString(), e)
            } finally {
                _trackDownloadProgress.value -= track.id
            }
        }
    }

    fun loadTrackMetadata(track: Track) {
        if (track.metadata == null) viewModelScope.launch(Dispatchers.IO) { getTrackMetadata(track) }
    }

    fun playOrPause(track: Track) = viewModelScope.launch {
        track.playUri?.let { playerRepo.playOrPause(it) }
    }

    fun removeFromLibrary() = viewModelScope.launch {
        _album.value?.let { repo.deleteAlbumWithTracks(it) }
    }

    fun saveDownloadedAlbum(album: Album) {
        viewModelScope.launch {
            repo.moveAndSaveAlbum(album) { _downloadProgress.value = it }
            _downloadProgress.value = null
        }
    }

    private suspend fun fetchVideos() {
        if (_videos.value.isEmpty()) {
            _album.value?.youtubePlaylist?.let { playlist ->
                _videos.value = youtubeRepo.listPlaylistVideos(playlist = playlist, withMetadata = true)
            }
        }
    }

    private suspend fun getTrackMetadata(track: Track): TrackMetadata? {
        if (track.metadata != null) return track.metadata
        else {
            val youtubeMetadata =
                track.youtubeVideo?.metadata ?: track.youtubeVideo?.id?.let { youtubeRepo.getBestMetadata(it) }
            val metadata = youtubeMetadata?.toTrackMetadata()

            if (youtubeMetadata != null) {
                updateTrack(
                    track.copy(
                        metadata = metadata,
                        youtubeVideo = track.youtubeVideo?.copy(metadata = youtubeMetadata),
                    )
                )
            }
            return metadata
        }
    }

    private fun updateTrack(track: Track) {
        _album.value?.let { album ->
            _album.value = album.copy(
                tracks = album.tracks.map { if (it.id == track.id) track else it }
            )
        }
    }

}
