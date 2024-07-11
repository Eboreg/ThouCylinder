package us.huseli.thoucylinder.dataclasses.album

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import us.huseli.thoucylinder.dataclasses.artist.IAlbumArtistCredit
import us.huseli.thoucylinder.enums.AlbumType

@Immutable
data class AlbumUiState(
    override val albumId: String,
    override val albumType: AlbumType?,
    override val artistString: String? = null,
    override val artists: ImmutableList<IAlbumArtistCredit> = persistentListOf(),
    override val fullImageUri: String?,
    override val isDownloadable: Boolean = false,
    override val isInLibrary: Boolean,
    override val isLocal: Boolean,
    override val isOnYoutube: Boolean,
    override val isSaved: Boolean,
    override val isSelected: Boolean,
    override val musicBrainzReleaseGroupId: String?,
    override val musicBrainzReleaseId: String?,
    override val spotifyWebUrl: String?,
    override val thumbnailUri: String?,
    override val title: String,
    override val trackCount: Int?,
    override val yearString: String?,
    override val youtubeWebUrl: String?,
    val isPartiallyDownloaded: Boolean = false,
    val spotifyId: String?,
    val unplayableTrackCount: Int = 0,
    val youtubePlaylistId: String?,
) : IAlbumUiState {
    override val isPlayable: Boolean
        get() = isLocal || youtubePlaylistId != null

    override fun withIsSelected(value: Boolean) = copy(isSelected = value)
}
