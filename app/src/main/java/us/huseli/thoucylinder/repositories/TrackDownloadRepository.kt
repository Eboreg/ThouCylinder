package us.huseli.thoucylinder.repositories

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.DownloadTaskState
import us.huseli.thoucylinder.TrackDownloadTask
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.entities.Track
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackDownloadRepository @Inject constructor(private val settingsRepo: SettingsRepository) {
    private val _tasks = MutableStateFlow<List<TrackDownloadTask>>(emptyList())
    private val _runningTasks = MutableStateFlow<List<TrackDownloadTask>>(emptyList())
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val maxConcurrent = 3

    val tasks = _tasks.map { tasks -> tasks.sortedByDescending { it.started } }

    init {
        scope.launch {
            _runningTasks.collect { runningTasks ->
                if (runningTasks.size < maxConcurrent) {
                    _tasks.value.find { it.state.value == DownloadTaskState.CREATED }?.start()
                }
            }
        }
    }

    fun downloadTrack(
        track: Track,
        repos: Repositories,
        albumPojo: AbstractAlbumPojo? = null,
        onError: (Throwable) -> Unit = {},
        onFinish: (Track) -> Unit = {},
    ): TrackDownloadTask {
        val relativePath =
            albumPojo?.album?.getSubdir()?.let { "${settingsRepo.musicDownloadDirectory.value}/$it" }
                ?: settingsRepo.musicDownloadDirectory.value
        val task = TrackDownloadTask(
            scope = scope,
            track = track,
            albumPojo = albumPojo,
            repos = repos,
            relativePath = relativePath,
            onError = onError,
            onFinish = onFinish,
        )

        _tasks.value += task
        if (_runningTasks.value.size < maxConcurrent) task.start()
        scope.launch {
            task.state.collect { state ->
                if (state == DownloadTaskState.RUNNING) _runningTasks.value += task
                else _runningTasks.value -= task
            }
        }
        return task
    }
}
