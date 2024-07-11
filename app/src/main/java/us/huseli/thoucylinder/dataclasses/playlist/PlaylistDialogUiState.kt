package us.huseli.thoucylinder.dataclasses.playlist

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class PlaylistDialogUiState(
    val uiStates: ImmutableList<PlaylistUiState>,
    val trackIds: ImmutableList<String>,
    val isLoading: Boolean,
    val selectedPlaylistId: String? = null,
    val duplicateCount: Int = 0,
    val createPlaylist: Boolean = false,
)
