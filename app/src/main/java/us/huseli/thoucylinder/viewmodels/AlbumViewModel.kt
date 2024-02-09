package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.AlbumDownloadTask
import us.huseli.thoucylinder.Constants.NAV_ARG_ALBUM
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
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

    val albumArt: Flow<ImageBitmap?> =
        _albumCombo.map { it?.album?.albumArt?.getFullImageBitmap(context) }.distinctUntilChanged()
    val albumDownloadTask: Flow<AlbumDownloadTask?> = repos.youtube.albumDownloadTasks
        .map { tasks -> tasks.find { it.album.albumId == _albumId } }
        .distinctUntilChanged()
    val albumCombo = _albumCombo.asStateFlow()
    override val trackDownloadTasks = repos.download.tasks
        .map { tasks -> tasks.filter { it.track.albumId == _albumId } }
        .distinctUntilChanged()
    val albumNotFound = _albumNotFound.asStateFlow()
    val trackCombos = repos.track.flowTrackCombosByAlbumId(_albumId)

    init {
        unselectAllTrackCombos()

        viewModelScope.launch(Dispatchers.IO) {
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

    fun loadTrackMetadata(track: Track) {
        /**
         * On-demand fetch (and save, if necessary) of track metadata, because
         * we only want to load it when it is actually going to be used.
         */
        viewModelScope.launch(Dispatchers.IO) {
            ensureTrackMetadata(track)
        }
    }
}
