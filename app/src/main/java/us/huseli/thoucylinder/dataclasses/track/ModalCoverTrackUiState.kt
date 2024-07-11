package us.huseli.thoucylinder.dataclasses.track

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableCollection

@Immutable
data class ModalCoverTrackUiState(
    override val albumId: String?,
    override val albumTitle: String?,
    override val artistString: String?,
    override val artists: ImmutableCollection<Artist>,
    override val fullImageUrl: String?,
    override val id: String,
    override val isDownloadable: Boolean,
    override val isInLibrary: Boolean,
    override val isPlayable: Boolean,
    override val musicBrainzReleaseGroupId: String?,
    override val musicBrainzReleaseId: String?,
    override val spotifyId: String?,
    override val spotifyWebUrl: String?,
    override val thumbnailUrl: String?,
    override val title: String,
    override val youtubeWebUrl: String?,
    val durationMs: Long,
) : AbstractTrackUiState() {
    override val isSelected: Boolean
        get() = false
    override val trackId: String
        get() = id
}

@Immutable
data class ModalCoverTrackUiStateLight(
    val albumArtUri: String?,
    val artistString: String?,
    val title: String,
)
