package us.huseli.thoucylinder.dataclasses.track

import kotlinx.collections.immutable.ImmutableCollection
import us.huseli.thoucylinder.dataclasses.artist.IArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.ISavedArtistCredit
import us.huseli.thoucylinder.interfaces.IStringIdItem

abstract class AbstractTrackUiState : IStringIdItem {
    abstract val albumId: String?
    abstract val albumTitle: String?
    abstract val artistString: String?
    abstract val artists: ImmutableCollection<Artist>
    abstract val fullImageUrl: String?
    abstract val isDownloadable: Boolean
    abstract val isInLibrary: Boolean
    abstract val isPlayable: Boolean
    abstract val isSelected: Boolean
    abstract val musicBrainzReleaseGroupId: String?
    abstract val musicBrainzReleaseId: String?
    abstract val spotifyId: String?
    abstract val spotifyWebUrl: String?
    abstract val thumbnailUrl: String?
    abstract val title: String
    abstract val trackId: String
    abstract val youtubeWebUrl: String?

    data class Artist(val name: String, val id: String?) {
        companion object {
            fun fromArtistCredit(artistCredit: IArtistCredit): Artist {
                return Artist(
                    name = artistCredit.name,
                    id = if (artistCredit is ISavedArtistCredit) artistCredit.artistId else null,
                )
            }
        }
    }
}
