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
import us.huseli.thoucylinder.Constants.NAV_ARG_ALBUM
import us.huseli.thoucylinder.LoadStatus
import us.huseli.thoucylinder.dataclasses.Album
import us.huseli.thoucylinder.dataclasses.DownloadProgress
import us.huseli.thoucylinder.dataclasses.Track
import us.huseli.thoucylinder.dataclasses.TrackMetadata
import us.huseli.thoucylinder.dataclasses.YoutubeMetadata
import us.huseli.thoucylinder.repositories.LocalRepository
import us.huseli.thoucylinder.repositories.PlayerRepository
import us.huseli.thoucylinder.repositories.YoutubeRepository
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val repo: LocalRepository,
    playerRepo: PlayerRepository,
    private val youtubeRepo: YoutubeRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel(playerRepo, repo, youtubeRepo) {
    private val _albumId: UUID? = savedStateHandle.get<String>(NAV_ARG_ALBUM)?.let { UUID.fromString(it) }

    private val _album = MutableStateFlow<Album?>(null)
    private val _albumArt = MutableStateFlow<ImageBitmap?>(null)
    private val _albumArtLoadStatus = MutableStateFlow(LoadStatus.LOADING)
    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    private val _trackDownloadProgress = MutableStateFlow<Map<UUID, DownloadProgress>>(emptyMap())

    val album = _album.asStateFlow()
    val albumArt = _albumArt.asStateFlow()
    val albumArtLoadStatus = _albumArtLoadStatus.asStateFlow()
    val downloadProgress = _downloadProgress.asStateFlow()
    val trackDownloadProgress = _trackDownloadProgress.asStateFlow()

    private var downloadJob: Job? = null

    init {
        _albumId?.let { albumId ->
            viewModelScope.launch {
                combine(repo.libraryAlbums, repo.tempAlbums) { libraryAlbums, tempAlbums ->
                    libraryAlbums.plus(tempAlbums.values).find { it.albumId == albumId }
                }.filterNotNull().distinctUntilChanged().collect {
                    _album.value = it
                }
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
            repo.saveAlbum(
                album.copy(
                    tracks = album.tracks.map { track ->
                        track.copy(metadata = getTrackMetadata(track))
                    }
                )
            )
        }
    }

    fun cancelDownload() = downloadJob?.cancel()

    fun downloadAndSaveAlbum(album: Album) {
        _album.value = album

        downloadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val videos = album.youtubePlaylist?.let {
                    youtubeRepo.listPlaylistVideos(playlist = it, withMetadata = true)
                } ?: emptyList()
                val tracks = youtubeRepo.downloadTracks(
                    videos = videos,
                    progressCallback = { _downloadProgress.value = it },
                )

                val newAlbum = repo.moveTaggedAlbumToMediaStore(
                    album = album.copy(tracks = tracks, isLocal = true),
                    progressCallback = { _downloadProgress.value = it },
                )
                _downloadProgress.value = null
                repo.saveAlbum(newAlbum)
                _album.value = newAlbum
            } catch (e: Exception) {
                Log.e("download", e.toString(), e)
            } finally {
                _downloadProgress.value = null
                downloadJob = null
            }
        }
    }

    fun loadTrackMetadata(track: Track) {
        if (track.metadata == null) viewModelScope.launch(Dispatchers.IO) { getTrackMetadata(track) }
    }

    fun removeFromLibrary() = viewModelScope.launch {
        _album.value?.let { repo.deleteAlbumWithTracks(it) }
    }

    fun update(album: Album) {
        _album.value = album
        viewModelScope.launch {
            repo.tagAndUpdateAlbumWithTracks(ensureTrackMetadata(album))
        }
    }

    private suspend fun ensureTrackMetadata(album: Album): Album = album.copy(
        tracks = album.tracks.map { track ->
            val (metadata, youtubeMetadata) = getTrackAndYoutubeMetadata(track)
            track.copy(
                metadata = metadata,
                youtubeVideo = track.youtubeVideo?.copy(metadata = youtubeMetadata),
            )
        }
    )

    private suspend fun getTrackAndYoutubeMetadata(track: Track): Pair<TrackMetadata?, YoutubeMetadata?> {
        if (track.metadata != null && track.youtubeVideo?.metadata != null)
            return Pair(track.metadata, track.youtubeVideo.metadata)
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
        return Pair(metadata, youtubeMetadata)
    }

    private suspend fun getTrackMetadata(track: Track): TrackMetadata? =
        track.metadata ?: getTrackAndYoutubeMetadata(track).first

    /** Does not save to database. */
    private fun updateTrack(track: Track) {
        _album.value?.let { album ->
            _album.value = album.copy(
                tracks = album.tracks.map { if (it.id == track.id) track else it }
            )
        }
    }
}
