package us.huseli.thoucylinder

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anggrayudi.storage.file.extension
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
import us.huseli.thoucylinder.dataclasses.extractTrackMetadata
import java.io.File
import java.io.FileOutputStream
import java.time.Instant

enum class DownloadTaskState { CREATED, RUNNING, CANCELLED, FINISHED, ERROR }

@Suppress("unused")
enum class DownloadStatus(@StringRes val stringId: Int) {
    STARTING(R.string.starting),
    DOWNLOADING(R.string.downloading),
    CONVERTING(R.string.converting),
    MOVING(R.string.moving),
    TAGGING(R.string.tagging),
    SAVING(R.string.saving),
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
    private val dirDocumentFile: DocumentFile,
    private val repos: Repositories,
    val albumPojo: AbstractAlbumPojo? = null,
    private val onError: (Throwable) -> Unit = {},
    private val onFinish: (Track) -> Unit = {},
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

    @Suppress("MemberVisibilityCanBePrivate")
    var updated: Instant = Instant.now()
        private set

    var error: Exception? = null
        private set

    fun cancel() {
        job?.cancel()
        _state.value = DownloadTaskState.CANCELLED
    }

    fun start(context: Context) {
        _state.value = DownloadTaskState.RUNNING
        started = Instant.now()

        job = scope.launch {
            try {
                val track = run(context)
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

    private suspend fun run(context: Context): Track {
        val youtubeMetadata = repos.youtube.getBestMetadata(track)
            ?: throw Error("Could not get Youtube metadata for $track")
        val basename = track.generateBasename(includeArtist = albumPojo == null)
        val filename = "$basename.${youtubeMetadata.fileExtension}"
        val tempFile = File(context.cacheDir, "${track.youtubeVideo!!.id}.${youtubeMetadata.fileExtension}")

        setProgress(0.0)
        setStatus(DownloadStatus.DOWNLOADING)
        repos.youtube.downloadVideo(
            url = youtubeMetadata.url,
            tempFile = tempFile,
            progressCallback = { setProgress(it * 0.7) },
        )

        setStatus(DownloadStatus.TAGGING)
        repos.localMedia.tagTrack(track = track, documentFile = DocumentFile.fromFile(tempFile), albumPojo = albumPojo)
        setProgress(0.8)

        setStatus(DownloadStatus.MOVING)
        dirDocumentFile.findFile(filename)?.delete()
        val documentFile = dirDocumentFile.createFile(youtubeMetadata.mimeType, basename)
            ?: throw Error("DocumentFile.fromTreeUri() returned null")

        if (documentFile.extension.isEmpty()) documentFile.renameTo(filename)

        context.contentResolver.openFileDescriptor(documentFile.uri, "w")?.use {
            FileOutputStream(it.fileDescriptor).use { outputStream ->
                outputStream.write(tempFile.readBytes())
            }
            setProgress(0.9)
        }

        setStatus(DownloadStatus.SAVING)
        val downloadedTrack = track.copy(
            isInLibrary = true,
            albumId = albumPojo?.album?.albumId ?: track.albumId,
            metadata = tempFile.extractTrackMetadata(),
            localUri = documentFile.uri,
        )
        repos.track.updateTrack(downloadedTrack)
        tempFile.delete()
        setProgress(1.0)

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
