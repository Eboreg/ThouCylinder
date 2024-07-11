package us.huseli.thoucylinder.dataclasses.album

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class EditAlbumUiState(
    val albumId: String,
    val artistNames: ImmutableList<String>,
    val artistString: String?,
    val title: String,
    val year: Int?,
)
