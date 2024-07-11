package us.huseli.thoucylinder.dataclasses.album

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import us.huseli.thoucylinder.dataclasses.artist.IAlbumArtistCredit
import us.huseli.thoucylinder.enums.AlbumType

data class ImportableAlbumUiState(
    override val albumId: String,
    override val albumType: AlbumType?,
    override val fullImageUri: String?,
    override val isInLibrary: Boolean,
    override val isLocal: Boolean,
    override val isOnYoutube: Boolean,
    override val isSaved: Boolean,
    override val musicBrainzReleaseGroupId: String?,
    override val musicBrainzReleaseId: String?,
    override val spotifyWebUrl: String?,
    override val thumbnailUri: String?,
    override val title: String,
    override val trackCount: Int?,
    override val yearString: String?,
    override val youtubeWebUrl: String?,
    override val artistString: String? = null,
    override val artists: ImmutableList<IAlbumArtistCredit> = persistentListOf(),
    override val isDownloadable: Boolean = false,
    override val isPlayable: Boolean = false,
    override val isSelected: Boolean = false,
    val importError: String? = null,
    val playCount: Int?,
) : IAlbumUiState {
    override fun withIsSelected(value: Boolean): ImportableAlbumUiState = copy(isSelected = value)
}
