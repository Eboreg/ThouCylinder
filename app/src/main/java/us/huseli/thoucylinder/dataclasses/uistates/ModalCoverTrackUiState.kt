package us.huseli.thoucylinder.dataclasses.uistates

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.collections.immutable.ImmutableCollection
import us.huseli.thoucylinder.dataclasses.abstr.AbstractArtist

@Immutable
data class ModalCoverTrackUiState(
    val artistString: String?,
    val artists: ImmutableCollection<AbstractArtist>,
    val durationMs: Long?,
    val fullImage: ImageBitmap?,
    val isDownloadable: Boolean,
    val isInLibrary: Boolean,
    val isPlayable: Boolean,
    val spotifyWebUrl: String?,
    val title: String,
    val trackId: String,
    val youtubeWebUrl: String?,
)
@Immutable
data class ModalCoverTrackUiStateLight(
    val fullImage: ImageBitmap?,
    val title: String,
    val trackArtistString: String?,
)
