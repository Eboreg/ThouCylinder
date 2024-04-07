package us.huseli.thoucylinder.repositories

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.DownloadTaskState
import us.huseli.thoucylinder.TrackDownloadTask
import us.huseli.thoucylinder.dataclasses.abstr.AbstractArtistCredit
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackDownloadRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val _tasks = MutableStateFlow<List<TrackDownloadTask>>(emptyList())
    private val _runningTasks = MutableStateFlow<List<TrackDownloadTask>>(emptyList())
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val maxConcurrent = 3

    val tasks = _tasks.map { tasks -> tasks.sortedByDescending { it.started }.toImmutableList() }

    init {
        scope.launch {
            _runningTasks.collect { runningTasks ->
                if (runningTasks.size < maxConcurrent) {
                    _tasks.value.find { it.state.value == DownloadTaskState.CREATED }?.start(context)
                }
            }
        }
    }

    fun downloadTrack(
        state: Track.ViewState,
        repos: Repositories,
        directory: DocumentFile,
        onError: (Throwable) -> Unit = {},
        onFinish: (Track) -> Unit = {},
    ): TrackDownloadTask {
        return downloadTrack(
            track = state.track,
            trackArtists = state.trackArtists,
            repos = repos,
            directory = directory,
            onError = onError,
            onFinish = onFinish,
            album = state.album,
            albumArtists = state.albumArtists,
        )
    }

    fun downloadTrack(
        track: Track,
        trackArtists: List<AbstractArtistCredit>,
        repos: Repositories,
        directory: DocumentFile,
        album: Album? = null,
        albumArtists: List<AbstractArtistCredit>? = null,
        onError: (Throwable) -> Unit = {},
        onFinish: (Track) -> Unit = {},
    ): TrackDownloadTask {
        val task = TrackDownloadTask(
            track = track,
            scope = scope,
            repos = repos,
            onError = onError,
            onFinish = onFinish,
            directory = directory,
            trackArtists = trackArtists,
            album = album,
            albumArtists = albumArtists,
        )

        _tasks.value += task
        if (_runningTasks.value.size < maxConcurrent) task.start(context)
        scope.launch {
            task.state.collect { state ->
                if (state == DownloadTaskState.RUNNING) _runningTasks.value += task
                else _runningTasks.value -= task
            }
        }
        return task
    }
}
