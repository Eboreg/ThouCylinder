package us.huseli.thoucylinder.dataclasses.album

import kotlinx.collections.immutable.ImmutableList
import us.huseli.thoucylinder.dataclasses.artist.IAlbumArtistCredit
import us.huseli.thoucylinder.enums.AlbumType
import us.huseli.thoucylinder.interfaces.IAlbumArtOwner
import us.huseli.thoucylinder.interfaces.IHasMusicBrainzIds
import us.huseli.thoucylinder.interfaces.IStringIdItem

interface IAlbumUiState : IStringIdItem, IAlbumArtOwner, IHasMusicBrainzIds {
    val albumId: String
    val artists: ImmutableList<IAlbumArtistCredit>
    val artistString: String?
    val albumType: AlbumType?
    override val fullImageUrl: String?
    val isDownloadable: Boolean
    val isInLibrary: Boolean
    val isLocal: Boolean
    val isOnYoutube: Boolean
    val isPlayable: Boolean
    val isSaved: Boolean
    val isSelected: Boolean
    override val musicBrainzReleaseGroupId: String?
    override val musicBrainzReleaseId: String?
    val spotifyWebUrl: String?
    override val thumbnailUrl: String?
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
