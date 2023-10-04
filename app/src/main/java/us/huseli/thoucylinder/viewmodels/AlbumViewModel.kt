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
            combine(repo.getAlbumWithSongs(_albumId), repo.tempAlbumPojos) { pojo, tempPojos ->
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

    fun addToLibrary(pojo: AlbumWithTracksPojo) {
        _albumPojo.value = pojo
        viewModelScope.launch(Dispatchers.IO) {
            repo.saveAlbum(
                pojo.copy(
                    tracks = pojo.tracks.map { track ->
                        track.copy(metadata = getTrackMetadata(track))
                    }
                )
            )
        }
    }

    fun cancelDownload() = downloadJob?.cancel()

    fun delete() = viewModelScope.launch(Dispatchers.IO) {
        if (BuildConfig.DEBUG) {
            _albumPojo.value?.also { repo.deleteAlbumWithTracks(it) }
        }
    }

    fun downloadAndSaveAlbum(pojo: AlbumWithTracksPojo) {
        _albumPojo.value = pojo

        downloadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val tracks = youtubeRepo.downloadTracks(
                    tracks = pojo.tracks,
                    progressCallback = { _downloadProgress.value = it },
                )
                val newPojo = repo.moveTaggedAlbumToMediaStore(
                    pojo = pojo.copy(tracks = tracks, album = pojo.album.copy(isLocal = true)),
                    progressCallback = { _downloadProgress.value = it },
                )
                _downloadProgress.value = null
                repo.saveAlbum(newPojo)
                _albumPojo.value = newPojo
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

    fun removeFromLibrary() = viewModelScope.launch(Dispatchers.IO) {
        _albumPojo.value?.let { repo.deleteAlbumWithTracks(it) }
    }

    fun update(pojo: AlbumWithTracksPojo) {
        _albumPojo.value = pojo
        viewModelScope.launch(Dispatchers.IO) {
            repo.tagAndUpdateAlbumWithTracks(ensureTrackMetadata(pojo))
        }
    }

    private suspend fun ensureTrackMetadata(pojo: AlbumWithTracksPojo): AlbumWithTracksPojo = pojo.copy(
        tracks = pojo.tracks.map { track ->
            val (metadata, youtubeMetadata) = getTrackAndYoutubeMetadata(track)
            track.copy(
                metadata = metadata,
                youtubeVideo = track.youtubeVideo?.copy(metadata = youtubeMetadata),
            )
        }
    )

    private suspend fun getTrackAndYoutubeMetadata(track: Track): Pair<TrackMetadata?, YoutubeMetadata?> {
        var metadata = track.metadata
        var youtubeMetadata = track.youtubeVideo?.metadata

        if (track.youtubeVideo != null) {
            if (youtubeMetadata == null) youtubeMetadata = youtubeRepo.getBestMetadata(track.youtubeVideo.id)
            if (metadata == null) metadata = youtubeMetadata?.toTrackMetadata()
            updateTrack(
                track.copy(metadata = metadata, youtubeVideo = track.youtubeVideo.copy(metadata = youtubeMetadata))
            )
        }
        return Pair(metadata, youtubeMetadata)
    }

    private suspend fun getTrackMetadata(track: Track): TrackMetadata? =
        track.metadata ?: getTrackAndYoutubeMetadata(track).first

    /** Does not save to database. */
    private fun updateTrack(track: Track) {
        _albumPojo.value = _albumPojo.value?.let { pojo ->
            pojo.copy(
                tracks = pojo.tracks.map { if (it.id == track.id) track else it },
            )
        }
    }
}
