package us.huseli.thoucylinder.dataclasses.track

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableCollection

@Immutable
data class TrackUiState(
    override val albumId: String?,
    override val albumTitle: String?,
    override val artistString: String?,
    override val artists: ImmutableCollection<Artist>,
    override val fullImageUrl: String?,
    override val id: String,
    override val isDownloadable: Boolean,
    override val isInLibrary: Boolean,
    override val isPlayable: Boolean,
    override val isSelected: Boolean,
    override val musicBrainzReleaseGroupId: String? = null,
    override val musicBrainzReleaseId: String? = null,
    override val spotifyId: String?,
    override val spotifyWebUrl: String?,
    override val thumbnailUrl: String?,
    override val title: String,
    override val trackId: String,
    override val youtubeWebUrl: String?,
    val albumPosition: Int?,
    val discNumber: Int?,
    val durationMs: Long?,
    val year: Int?,
) : AbstractTrackUiState() {
    fun getSecondaryInfo(
        showAlbum: Boolean,
        showArtist: Boolean,
        showYear: Boolean,
        separator: String = " • ",
    ): String? {
        return listOfNotNull(
            artistString.takeIf { showArtist },
            albumTitle.takeIf { showAlbum },
            year.takeIf { showYear }?.toString(),
        ).takeIf { it.isNotEmpty() }?.joinToString(separator)
    }
}
