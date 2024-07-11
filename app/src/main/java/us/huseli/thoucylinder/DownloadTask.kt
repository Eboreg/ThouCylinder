package us.huseli.thoucylinder

import androidx.annotation.StringRes
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.dataclasses.album.Album
import us.huseli.thoucylinder.dataclasses.album.IAlbum
import us.huseli.thoucylinder.dataclasses.artist.IArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.joined
import us.huseli.thoucylinder.dataclasses.track.Track
import us.huseli.thoucylinder.dataclasses.track.extractTrackMetadata
import us.huseli.thoucylinder.repositories.Repositories
import java.io.File
import java.time.Instant

enum class DownloadTaskState { CREATED, RUNNING, CANCELLED, FINISHED, ERROR }

@Suppress("unused")
enum class DownloadStatus(@StringRes val stringId: Int) {
    WAITING(R.string.waiting),
    DOWNLOADING(R.string.downloading),
    CONVERTING(R.string.converting),
    CONVERTING_AND_TAGGING(R.string.converting_and_tagging),
    MOVING(R.string.moving),
    TAGGING(R.string.tagging),
    SAVING(R.string.saving),
}

abstract class AbstractDownloadUiState {
    abstract val isActive: Boolean
    abstract val progress: Float
}

abstract class AbstractDownloadTask {
    abstract val downloadProgress: Flow<Double>
    abstract val state: StateFlow<DownloadTaskState>

    open val isActive: Flow<Boolean>
        get() = state.map { it == DownloadTaskState.RUNNING }
}

class TrackDownloadTask(
    private val scope: CoroutineScope,
    val track: Track,
    val trackArtists: List<IArtistCredit>,
    private val directory: DocumentFile,
    private val repos: Repositories,
    val album: IAlbum? = null,
    val albumArtists: List<IArtistCredit>? = null,
    private val onError: (Throwable) -> Unit = {},
) : AbstractDownloadTask() {
    data class UiState(
        val trackId: String,
        override val isActive: Boolean,
        val status: DownloadStatus,
        override val progress: Float,
    ) : AbstractDownloadUiState()

    private var job: Job? = null
    private val _state = MutableStateFlow(DownloadTaskState.CREATED)
    private val _downloadStatus = MutableStateFlow(DownloadStatus.WAITING)
    private val _downloadProgress = MutableStateFlow(0.0)

    override val downloadProgress = _downloadProgress.asStateFlow()
    override val state = _state.asStateFlow()

    val albumArtThumbnailUrl: String?
        get() = album?.thumbnailUrl ?: track.thumbnailUrl

    val uiStateFlow: StateFlow<UiState?> =
        combine(isActive, _downloadStatus, _downloadProgress) { isActive, status, progress ->
            UiState(
                trackId = track.trackId,
                isActive = isActive,
                status = status,
                progress = progress.toFloat(),
            )
        }.distinctUntilChanged().stateIn(scope, started = SharingStarted.Lazily, initialValue = null)

    override val isActive: Flow<Boolean>
        get() = _state.map { it == DownloadTaskState.RUNNING || it == DownloadTaskState.CREATED }

    var started: Instant = Instant.now()
        private set

    var error: Exception? = null
        private set

    fun cancel() {
        job?.cancel()
        _state.value = DownloadTaskState.CANCELLED
    }

    fun start() {
        _state.value = DownloadTaskState.RUNNING
        started = Instant.now()

        job = scope.launch {
            try {
                run()
                _state.value = DownloadTaskState.FINISHED
            } catch (e: CancellationException) {
                _state.value = DownloadTaskState.CANCELLED
            } catch (e: Exception) {
                _state.value = DownloadTaskState.ERROR
                error = e
                onError(e)
            }
        }
    }

    private suspend fun run(): Track {
        val youtubeMetadata = track.youtubeVideo?.id?.let { repos.youtube.getBestMetadata(videoId = it) }
            ?: throw Exception("Could not get Youtube metadata for $track (youtubeVideo=${track.youtubeVideo})")
        val basename = track.generateBasename(
            includeArtist = album == null,
            artist = trackArtists.joined(),
        )

        setProgress(0.0)
        setStatus(DownloadStatus.DOWNLOADING)
        var tempFile: File = repos.youtube.downloadVideo(
            video = track.youtubeVideo,
            progressCallback = { setProgress(it * 0.7) },
        )

        setStatus(DownloadStatus.CONVERTING_AND_TAGGING)
        tempFile = repos.localMedia.convertAndTagTrack(
            tmpInFile = tempFile,
            extension = youtubeMetadata.containerFormat.audioFileExtension,
            track = track,
            trackArtists = trackArtists,
            album = album,
            albumArtists = albumArtists,
        )

        setProgress(0.8)
        setStatus(DownloadStatus.MOVING)
        val documentFile = repos.localMedia.copyTempAudioFile(
            basename = basename,
            tempFile = tempFile,
            mimeType = youtubeMetadata.containerFormat.shortMimeType,
            directory = directory,
        )

        setProgress(0.9)
        setStatus(DownloadStatus.SAVING)
        val metadata = tempFile.extractTrackMetadata()
        val downloadedTrack = track.copy(
            isInLibrary = true,
            albumId = album?.albumId ?: track.albumId,
            metadata = metadata,
            localUri = documentFile.uri.toString(),
            durationMs = metadata?.durationMs,
        )

        repos.track.upsertTrack(downloadedTrack)
        setProgress(1.0)
        tempFile.delete()

        return downloadedTrack
    }

    private fun setProgress(value: Double) {
        if (value != _downloadProgress.value) {
            _downloadProgress.value = value
        }
    }

    private fun setStatus(value: DownloadStatus) {
        if (value != _downloadStatus.value) {
            _downloadStatus.value = value
        }
    }

    override fun equals(other: Any?) = other is TrackDownloadTask && other.track == track

    override fun hashCode() = track.hashCode()
}

class AlbumDownloadTask(
    val album: Album,
    private val trackTasks: List<TrackDownloadTask>,
    private val onFinish: (Result) -> Unit = {},
) : AbstractDownloadTask() {
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val states = combine(trackTasks.map { it.state }) { it }.distinctUntilChanged()
    private val _state = MutableStateFlow(DownloadTaskState.CREATED)

    override val downloadProgress =
        combine(trackTasks.map { it.downloadProgress }) { it.average() }.distinctUntilChanged()
    override val state = _state.asStateFlow()

    data class UiState(
        val albumId: String,
        override val isActive: Boolean,
        override val progress: Float,
    ) : AbstractDownloadUiState()

    data class Result(
        val succeededTracks: List<Track>,
        val failedTracks: List<Track>,
    )

    val uiStateFlow: StateFlow<UiState?> = combine(isActive, downloadProgress) { isActive, progress ->
        UiState(
            albumId = album.albumId,
            isActive = isActive,
            progress = progress.toFloat(),
        )
    }.distinctUntilChanged().stateIn(scope, started = SharingStarted.Lazily, initialValue = null)

    init {
        scope.launch {
            states.map { states -> states.any { it == DownloadTaskState.RUNNING } }.transformWhile {
                emit(it)
                false
            }.collect { _state.value = DownloadTaskState.RUNNING }

            states.collect { states ->
                if (states.all { it == DownloadTaskState.FINISHED || it == DownloadTaskState.ERROR }) {
                    val succeeded = trackTasks.filter { it.state.value == DownloadTaskState.FINISHED }
                    val failed = trackTasks.filter { it.state.value == DownloadTaskState.ERROR }

                    _state.value = DownloadTaskState.FINISHED
                    onFinish(
                        Result(
                            succeededTracks = succeeded.map { it.track },
                            failedTracks = failed.map { it.track },
                        )
                    )
                }
            }
        }
    }

    fun cancel() {
        _state.value = DownloadTaskState.CANCELLED
        trackTasks.forEach { it.cancel() }
    }
}
