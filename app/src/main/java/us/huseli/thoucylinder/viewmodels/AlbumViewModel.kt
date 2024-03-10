package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.AlbumDownloadTask
import us.huseli.thoucylinder.Constants.NAV_ARG_ALBUM
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.ProgressData
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.launchOnIOThread
import us.huseli.thoucylinder.umlautify
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val repos: Repositories,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext context: Context,
) : AbstractTrackListViewModel("AlbumViewModel", repos) {
    private val _albumId: UUID = UUID.fromString(savedStateHandle.get<String>(NAV_ARG_ALBUM)!!)
    private val _albumCombo = MutableStateFlow<AlbumWithTracksCombo?>(null)
    private val _albumNotFound = MutableStateFlow(false)
    private val _importProgress = MutableStateFlow<ProgressData?>(null)

    val albumArt: Flow<ImageBitmap?> =
        _albumCombo.map { it?.album?.albumArt?.getFullImageBitmap(context) }.distinctUntilChanged()
    val albumDownloadTask: Flow<AlbumDownloadTask?> = repos.youtube.albumDownloadTasks
        .map { tasks -> tasks.find { it.album.albumId == _albumId } }
        .distinctUntilChanged()
    val albumCombo = _albumCombo.asStateFlow()
    override val trackDownloadTasks = repos.download.tasks
        .map { tasks -> tasks.filter { it.trackCombo.track.albumId == _albumId } }
        .distinctUntilChanged()
    val albumNotFound = _albumNotFound.asStateFlow()
    val importProgress = _importProgress.asStateFlow()

    init {
        unselectAllTracks()

        launchOnIOThread {
            repos.album.flowAlbumWithTracks(_albumId).distinctUntilChanged().collect { combo ->
                if (combo != null) {
                    _albumNotFound.value = false
                    _albumCombo.value = combo
                } else {
                    _albumNotFound.value = true
                }
            }
        }
    }

    fun matchUnplayableTracks(context: Context) = launchOnIOThread {
        _albumCombo.value?.also { combo ->
            val unplayableTrackIds = combo.trackCombos.filter { !it.track.isPlayable }.map { it.track.trackId }
            val progressData = ProgressData(text = context.getString(R.string.matching))

            _importProgress.value = progressData

            val match = repos.youtube.getBestAlbumMatch(combo) { progress ->
                _importProgress.value = progressData.copy(progress = progress * 0.5)
            }
            val matchedCombo = match?.albumCombo
            val updatedTracks =
                matchedCombo?.trackCombos?.map { it.track }?.filter { unplayableTrackIds.contains(it.trackId) }
                    ?: emptyList()

            if (updatedTracks.isNotEmpty()) {
                _importProgress.value = progressData.copy(progress = 0.5, text = context.getString(R.string.importing))
                repos.track.updateTracks(updatedTracks.map { ensureTrackMetadata(it, commit = false) })
                _importProgress.value = progressData.copy(progress = 0.9, text = context.getString(R.string.importing))
                // So youtubePlaylist gets saved:
                matchedCombo?.album?.also { repos.album.updateAlbum(it) }
                SnackbarEngine.addInfo(
                    context.resources.getQuantityString(
                        R.plurals.x_tracks_matched_and_updated,
                        updatedTracks.size,
                        updatedTracks.size,
                    ).umlautify()
                )
            } else {
                SnackbarEngine.addError(context.getString(R.string.no_tracks_could_be_matched).umlautify())
            }

            _importProgress.value = null
        }
    }

    override fun getTrackSelectionCallbacks(appCallbacks: AppCallbacks, context: Context): TrackSelectionCallbacks {
        return super.getTrackSelectionCallbacks(appCallbacks, context).copy(
            onSelectAllClick = {
                repos.track.selectTrackIds(
                    selectionKey = "AlbumViewModel",
                    trackIds = _albumCombo.value?.trackCombos?.map { it.track.trackId } ?: emptyList(),
                )
            }
        )
    }
}
