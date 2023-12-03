package us.huseli.thoucylinder

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.repositories.Repositories
import java.time.Instant

enum class DownloadTaskState { CREATED, RUNNING, CANCELLED, FINISHED, ERROR }

enum class DownloadStatus(@StringRes val stringId: Int) {
    STARTING(R.string.starting),
    DOWNLOADING(R.string.downloading),
    CONVERTING(R.string.converting),
    MOVING(R.string.moving),
}

abstract class AbstractDownloadTask {
    abstract val downloadProgress: Flow<Double>
    abstract val state: Flow<DownloadTaskState>

    val isActive: Flow<Boolean>
        get() = state.map { it == DownloadTaskState.RUNNING }
}

@Composable
fun getDownloadProgress(downloadTask: AbstractDownloadTask?): Pair<Double?, Boolean> = Pair(
    downloadTask?.downloadProgress?.collectAsStateWithLifecycle(0.0)?.value,
    downloadTask?.isActive?.collectAsStateWithLifecycle(false)?.value ?: false,
)

@Composable
fun getDownloadProgress(downloadTaskState: State<AbstractDownloadTask?>): Pair<Double?, Boolean> =
    getDownloadProgress(downloadTask = downloadTaskState.value)

class TrackDownloadTask(
    private val scope: CoroutineScope,
    val track: Track,
    val relativePath: String,
    val repos: Repositories,
    val albumPojo: AbstractAlbumPojo? = null,
    val onError: (Throwable) -> Unit = {},
    val onFinish: (Track) -> Unit = {},
) : AbstractDownloadTask() {
    private var job: Job? = null
    private val _state = MutableStateFlow(DownloadTaskState.CREATED)
    private val _downloadStatus = MutableStateFlow(DownloadStatus.STARTING)
    private val _downloadProgress = MutableStateFlow(0.0)

    override val downloadProgress = _downloadProgress.asStateFlow()
    override val state = _state.asStateFlow()
    val downloadStatus = _downloadStatus.asStateFlow()

    var started: Instant = Instant.now()
        private set

    var updated: Instant = Instant.now()
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
                val track = run()
                _state.value = DownloadTaskState.FINISHED
                onFinish(track)
            } catch (e: CancellationException) {
                _state.value = DownloadTaskState.CANCELLED
            } catch (e: Exception) {
                _state.value = DownloadTaskState.ERROR
                error = e
                onError(e)
                throw e
            }
        }
    }

    private suspend fun run(): Track {
        // val relativePath = albumPojo?.album?.getMediaStoreSubdir()?.let { "${Environment.DIRECTORY_MUSIC}/$it" }
        //     ?: Environment.DIRECTORY_MUSIC

        setProgress(0.0)

        val tempFile = repos.youtube.downloadVideo(
            video = track.youtubeVideo!!,
            progressCallback = { setProgress(it * 0.9) },
            statusCallback = { setStatus(it) },
        )
        val downloadedTrack = repos.mediaStore.moveTaggedTrackToMediaStore(
            track = track,
            tempFile = tempFile,
            relativePath = relativePath,
            albumPojo = albumPojo,
            progressCallback = { setProgress((it * 0.1) + 0.9) },
            statusCallback = { setStatus(it) },
        )

        setProgress(1.0)
        repos.room.updateTrack(downloadedTrack)
        return downloadedTrack
    }

    private fun setProgress(value: Double) {
        if (value != _downloadProgress.value) {
            _downloadProgress.value = value
            updated = Instant.now()
        }
    }

    private fun setStatus(value: DownloadStatus) {
        if (value != _downloadStatus.value) {
            _downloadStatus.value = value
            updated = Instant.now()
        }
    }

    override fun equals(other: Any?) =
        other is TrackDownloadTask && other.track == track && other.state.value == state.value

    override fun hashCode() = 31 * track.hashCode() + state.value.hashCode()
}

class AlbumDownloadTask(
    val album: Album,
    private val trackTasks: List<TrackDownloadTask>,
    private val onFinish: (hasErrors: Boolean) -> Unit = {},
) : AbstractDownloadTask() {
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val states = combine(trackTasks.map { it.state }) { it }.distinctUntilChanged()
    private val _state = MutableStateFlow(DownloadTaskState.CREATED)

    override val downloadProgress =
        combine(trackTasks.map { it.downloadProgress }) { it.average() }.distinctUntilChanged()
    override val state = _state.asStateFlow()

    init {
        scope.launch {
            states.map { states -> states.any { it == DownloadTaskState.RUNNING } }.transformWhile {
                emit(it)
                false
            }.collect { _state.value = DownloadTaskState.RUNNING }

            states.collect { states ->
                if (states.all { it == DownloadTaskState.FINISHED }) {
                    _state.value = DownloadTaskState.FINISHED
                    onFinish(false)
                } else if (states.all { it == DownloadTaskState.FINISHED || it == DownloadTaskState.ERROR }) {
                    _state.value = DownloadTaskState.FINISHED
                    onFinish(true)
                }
            }
        }
    }

    fun cancel() {
        _state.value = DownloadTaskState.CANCELLED
        trackTasks.forEach { it.cancel() }
    }
}
