package us.huseli.thoucylinder.dataclasses.uistates

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistPojo

@Immutable
data class PlaylistDialogUiState(
    val playlists: ImmutableList<PlaylistPojo>,
    val trackIds: ImmutableList<String>,
    val selectedPlaylistId: String? = null,
    val duplicateCount: Int = 0,
    val createPlaylist: Boolean = false,
)
