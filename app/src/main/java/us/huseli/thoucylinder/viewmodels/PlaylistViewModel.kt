package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.Constants.NAV_ARG_PLAYLIST
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.combos.PlaylistTrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.launchOnIOThread
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repos: Repositories,
) : AbstractTrackListViewModel("PlaylistViewModel", repos) {
    private val _trackCombos = MutableStateFlow<List<PlaylistTrackCombo>>(emptyList())

    val playlistId: UUID = UUID.fromString(savedStateHandle.get<String>(NAV_ARG_PLAYLIST)!!)
    val playlist = repos.playlist.flowPlaylist(playlistId)
    val trackCombos = _trackCombos.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repos.playlist.flowPlaylistTracks(playlistId).collect { combos ->
                _trackCombos.value = combos
            }
        }
    }

    fun onMoveTrack(from: Int, to: Int) {
        _trackCombos.value = _trackCombos.value.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun onMoveTrackFinished(from: Int, to: Int) = viewModelScope.launch(Dispatchers.IO) {
        repos.playlist.movePlaylistTrack(playlistId, from, to)
    }

    fun playPlaylist(startAt: PlaylistTrackCombo? = null) = playPlaylist(playlistId, startAt?.track?.trackId)

    fun removeTrackCombos(ids: List<UUID>) = launchOnIOThread {
        repos.playlist.removePlaylistTracks(playlistId, ids)
        unselectTracks(ids)
    }

    override suspend fun listSelectedTrackCombos(): List<PlaylistTrackCombo> =
        repos.playlist.listPlaylistTrackCombosById(selectedTrackIds.value)

    override suspend fun listSelectedTracks(): List<Track> = listSelectedTrackCombos().map { it.track }

    override fun getTrackSelectionCallbacks(appCallbacks: AppCallbacks, context: Context): TrackSelectionCallbacks {
        return super.getTrackSelectionCallbacks(appCallbacks, context).copy(
            onSelectAllClick = {
                repos.track.selectTrackIds(
                    selectionKey = "PlaylistViewModel",
                    trackIds = _trackCombos.value.map { it.track.trackId },
                )
            }
        )
    }
}
