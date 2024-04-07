package us.huseli.thoucylinder

import android.content.Context
import androidx.annotation.StringRes
import androidx.documentfile.provider.DocumentFile
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
import us.huseli.thoucylinder.dataclasses.abstr.AbstractArtistCredit
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.extractTrackMetadata
import us.huseli.thoucylinder.repositories.Repositories
import java.io.File
import java.io.FileOutputStream
import java.time.Instant

enum class DownloadTaskState { CREATED, RUNNING, CANCELLED, FINISHED, ERROR }

@Suppress("unused")
enum class DownloadStatus(@StringRes val stringId: Int) {
    WAITING(R.string.waiting),
    DOWNLOADING(R.string.downloading),
    CONVERTING(R.string.converting),
    MOVING(R.string.moving),
    TAGGING(R.string.tagging),
    SAVING(R.string.saving),
}

abstract class AbstractDownloadTask {
    abstract val downloadProgress: Flow<Double>
    abstract val state: Flow<DownloadTaskState>

    open val isActive: Flow<Boolean>
        get() = state.map { it == DownloadTaskState.RUNNING }
}

class TrackDownloadTask(
    private val scope: CoroutineScope,
    val track: Track,
    val trackArtists: List<AbstractArtistCredit>,
    private val directory: DocumentFile,
    private val repos: Repositories,
    val album: Album? = null,
    val albumArtists: List<AbstractArtistCredit>? = null,
    private val onError: (Throwable) -> Unit = {},
    private val onFinish: (Track) -> Unit = {},
) : AbstractDownloadTask() {
    data class ViewState(
        val trackId: String,
        val isActive: Boolean,
        val status: DownloadStatus,
        val progress: Float,
    )

    private var job: Job? = null
    private val _state = MutableStateFlow(DownloadTaskState.CREATED)
    private val _downloadStatus = MutableStateFlow(DownloadStatus.WAITING)
    private val _downloadProgress = MutableStateFlow(0.0)

    override val downloadProgress = _downloadProgress.asStateFlow()
    override val state = _state.asStateFlow()

    val viewState: Flow<ViewState?> =
        combine(isActive, _downloadStatus, _downloadProgress) { isActive, status, progress ->
            ViewState(
                trackId = track.trackId,
                isActive = isActive,
                status = status,
                progress = progress.toFloat(),
            )
        }

    override val isActive: Flow<Boolean>
        get() = _state.map { it == DownloadTaskState.RUNNING || it == DownloadTaskState.CREATED }

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
            }
        }
    }

    private suspend fun run(context: Context): Track {
        val youtubeMetadata = repos.youtube.getBestMetadata(track)
            ?: throw Exception("Could not get Youtube metadata for $track (youtubeVideo=${track.youtubeVideo})")
        val basename = track.generateBasename(
            includeArtist = album == null,
            artist = trackArtists.joined(),
        )
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
        repos.localMedia.tagTrack(
            track = track,
            albumArtists = albumArtists,
            documentFile = DocumentFile.fromFile(tempFile),
            trackArtists = trackArtists,
            album = album,
        )
        setProgress(0.8)

        setStatus(DownloadStatus.MOVING)
        directory.findFile(filename)?.delete()
        val documentFile = directory.createFile(youtubeMetadata.mimeType, basename)
            ?: throw Exception(
                "DocumentFile.createFile() returned null. mimeType=${youtubeMetadata.mimeType}, " +
                    "basename=$basename, directory=$directory"
            )

        if (documentFile.extension.isEmpty()) documentFile.renameTo(filename)

        context.contentResolver.openFileDescriptor(documentFile.uri, "w")?.use {
            FileOutputStream(it.fileDescriptor).use { outputStream ->
                outputStream.write(tempFile.readBytes())
            }
            setProgress(0.9)
        }

        setStatus(DownloadStatus.SAVING)
        val metadata = tempFile.extractTrackMetadata()
        val downloadedTrack = track.copy(
            isInLibrary = true,
            albumId = album?.albumId ?: track.albumId,
            metadata = metadata,
            localUri = documentFile.uri.toString(),
            durationMs = metadata?.durationMs,
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

    data class ViewState(
        val albumId: String,
        val isActive: Boolean,
        val progress: Float,
    )

    val viewState: Flow<ViewState?> = combine(isActive, downloadProgress) { isActive, progress ->
        ViewState(
            albumId = album.albumId,
            isActive = isActive,
            progress = progress.toFloat(),
        )
    }

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
