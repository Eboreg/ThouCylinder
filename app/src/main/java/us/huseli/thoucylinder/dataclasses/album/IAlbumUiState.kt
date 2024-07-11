package us.huseli.thoucylinder.dataclasses.album

import kotlinx.collections.immutable.ImmutableList
import us.huseli.thoucylinder.dataclasses.artist.IAlbumArtistCredit
import us.huseli.thoucylinder.enums.AlbumType
import us.huseli.thoucylinder.interfaces.IStringIdItem

interface IAlbumUiState : IStringIdItem {
    val albumId: String
    val artists: ImmutableList<IAlbumArtistCredit>
    val artistString: String?
    val albumType: AlbumType?
    val fullImageUri: String?
    val isDownloadable: Boolean
    val isInLibrary: Boolean
    val isLocal: Boolean
    val isOnYoutube: Boolean
    val isPlayable: Boolean
    val isSaved: Boolean
    val isSelected: Boolean
    val musicBrainzReleaseGroupId: String?
    val musicBrainzReleaseId: String?
    val spotifyWebUrl: String?
    val thumbnailUri: String?
    val title: String
    val trackCount: Int?
    val yearString: String?
    val youtubeWebUrl: String?

    override val id: String
        get() = albumId

    fun matchesSearchTerm(term: String): Boolean {
        val words = term.lowercase().split(Regex(" +"))

        return words.all {
            artistString?.lowercase()?.contains(it) == true ||
                title.lowercase().contains(it) ||
                yearString?.contains(it) == true
        }
    }

    fun withIsSelected(value: Boolean): IAlbumUiState
}
