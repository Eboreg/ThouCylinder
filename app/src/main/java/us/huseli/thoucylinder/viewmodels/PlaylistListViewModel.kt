package us.huseli.thoucylinder.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import us.huseli.thoucylinder.dataclasses.playlist.Playlist
import us.huseli.thoucylinder.dataclasses.playlist.PlaylistUiState
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class PlaylistListViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractBaseViewModel() {
    private val _isLoading = MutableStateFlow(true)

    val isEmpty: StateFlow<Boolean> = combine(_isLoading, repos.playlist.playlistUiStates) { isLoading, states ->
        states.isEmpty() && !isLoading
    }.stateWhileSubscribed(false)
    val isLoading = _isLoading.asStateFlow()
    val playlistUiStates: StateFlow<ImmutableList<PlaylistUiState>> = repos.playlist.playlistUiStates
        .onStart { _isLoading.value = true }
        .onEach { _isLoading.value = false }
        .stateWhileSubscribed(persistentListOf())

    fun addTracksToPlaylist(
        playlistId: String,
        trackIds: Collection<String>,
        includeDuplicates: Boolean = true,
        onPlaylistClick: () -> Unit,
    ) = managers.playlist.addTracksToPlaylist(
        playlistId = playlistId,
        trackIds = trackIds,
        includeDuplicates = includeDuplicates,
        onPlaylistClick = onPlaylistClick,
    )

    fun createPlaylist(playlist: Playlist, addTracks: Collection<String>, onPlaylistClick: () -> Unit) =
        managers.playlist.createPlaylist(playlist, addTracks, onPlaylistClick)

    fun deletePlaylist(playlistId: String, onGotoPlaylistClick: () -> Unit) =
        managers.playlist.deletePlaylist(playlistId, onGotoPlaylistClick)

    suspend fun getDuplicatePlaylistTrackCount(playlistId: String, trackIds: Collection<String>) =
        repos.playlist.getDuplicatePlaylistTrackCount(playlistId, trackIds)

    fun playPlaylist(playlistId: String) = managers.player.playPlaylist(playlistId)

    fun renamePlaylist(playlistId: String, newName: String) = managers.playlist.renamePlaylist(playlistId, newName)
}
