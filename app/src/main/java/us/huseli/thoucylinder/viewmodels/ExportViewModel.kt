package us.huseli.thoucylinder.viewmodels

import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.retaintheme.extensions.sanitizeFilename
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.ProgressData
import us.huseli.thoucylinder.dataclasses.track.TrackCombo
import us.huseli.thoucylinder.dataclasses.track.toPlainTrackCombos
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractBaseViewModel() {
    private val _progress = MutableStateFlow(ProgressData())
    private val _trackCombos = MutableStateFlow<ImmutableList<TrackCombo>>(persistentListOf())
    private var _title: String? = null

    val progress = _progress.asStateFlow()
    val trackCombos = _trackCombos.asStateFlow()

    fun clear() {
        _trackCombos.value = persistentListOf()
        _title = null
    }

    fun exportTracksAsXspf(
        trackCombos: Collection<TrackCombo>,
        outputUri: Uri,
        dateTime: OffsetDateTime,
        onFinish: () -> Unit,
    ) {
        launchOnIOThread {
            val success = managers.library.exportTracksAsXspf(
                trackCombos = prepareTrackCombos(trackCombos),
                outputUri = outputUri,
                dateTime = dateTime,
                title = _title,
            )

            clear()
            clearProgress()
            repos.message.onExportPlaylist(success = success, path = outputUri.path)
            onFinish()
        }
    }

    fun exportTracksAsJspf(
        trackCombos: Collection<TrackCombo>,
        outputUri: Uri,
        dateTime: OffsetDateTime,
        onFinish: () -> Unit,
    ) {
        launchOnIOThread {
            val success = managers.library.exportTracksAsJspf(
                trackCombos = prepareTrackCombos(trackCombos),
                outputUri = outputUri,
                dateTime = dateTime,
                title = _title,
            )

            clear()
            clearProgress()
            repos.message.onExportPlaylist(success = success, path = outputUri.path)
            onFinish()
        }
    }

    fun getDateTime(): OffsetDateTime = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS)

    fun getJspfFilename(dateTime: OffsetDateTime): String = getBasename(dateTime) + ".json"

    fun getXspfFilename(dateTime: OffsetDateTime) = getBasename(dateTime) + ".xspf"

    fun setAlbumIds(albumIds: Collection<String>) {
        launchOnIOThread {
            _trackCombos.value = repos.track.listTrackCombosByAlbumId(*albumIds.toTypedArray()).toImmutableList()
        }
    }

    fun setPlaylistId(playlistId: String) {
        launchOnIOThread {
            _title = repos.playlist.getPlaylist(playlistId)?.name
            _trackCombos.value = repos.playlist.listPlaylistTrackCombos(playlistId)
                .toPlainTrackCombos()
                .toImmutableList()
        }
    }

    fun setTrackIds(trackIds: Collection<String>) {
        launchOnIOThread {
            _trackCombos.value = repos.track.listTrackCombosById(trackIds).toImmutableList()
        }
    }

    private fun clearProgress() {
        _progress.value = ProgressData()
    }

    private fun formatDateTime(dateTime: OffsetDateTime): String =
        dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmss"))

    private fun getBasename(dateTime: OffsetDateTime): String =
        _title?.sanitizeFilename() ?: "fistopy-${formatDateTime(dateTime)}"

    private suspend fun prepareTrackCombos(trackCombos: Collection<TrackCombo>): List<TrackCombo> {
        return trackCombos.mapIndexed { index, combo ->
            combo.copy(track = managers.library.ensureTrackMetadata(track = combo.track)).also {
                _progress.value = ProgressData(
                    progress = index.toDouble() / trackCombos.size,
                    isActive = true,
                    stringRes = R.string.exporting,
                )
            }
        }
    }
}
