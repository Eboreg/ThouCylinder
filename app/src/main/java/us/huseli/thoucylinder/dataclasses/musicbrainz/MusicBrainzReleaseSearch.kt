package us.huseli.thoucylinder.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbum
import us.huseli.thoucylinder.enums.AlbumType
import kotlin.time.Duration

data class MusicBrainzReleaseSearch(
    val count: Int,
    val offset: Int,
    val releases: List<Release>,
) {
    data class Release(
        @SerializedName("artist-credit")
        override val artistCredit: List<MusicBrainzArtistCredit>,
        override val country: String?,
        override val date: String?,
        override val id: String,
        val media: List<MusicBrainzSimplifiedMedia>,
        val packaging: String?,
        @SerializedName("release-group")
        val releaseGroup: ReleaseGroup,
        override val status: MusicBrainzReleaseStatus?,
        override val title: String,
        @SerializedName("track-count")
        override val trackCount: Int,
    ) : AbstractMusicBrainzRelease() {
        data class ReleaseGroup(
            override val id: String,
            @SerializedName("primary-type")
            val primaryType: MusicBrainzReleaseGroupPrimaryType?,
            @SerializedName("primary-type-id")
            val primaryTypeId: String?,
            val title: String,
            @SerializedName("type-id")
            val typeId: String?,
        ) : AbstractMusicBrainzItem()

        override val albumType: AlbumType?
            get() = super.albumType ?: releaseGroup.primaryType?.albumType

        override val releaseGroupId: String
            get() = releaseGroup.id

        fun matches(artist: String?, album: String): Boolean {
            val artistCredits = artistCredit.joined()
            val albumMatch = album.contains(title, true) || title.contains(album, true)
            val artistMatch = artist?.let {
                it.contains(artistCredits, true) || artistCredits.contains(it, true)
            } ?: album.contains(artistCredits, true)

            return albumMatch && artistMatch
        }

        override fun toAlbum(isLocal: Boolean, isInLibrary: Boolean, albumId: String?): UnsavedAlbum =
            super.toAlbum(isLocal, isInLibrary, albumId).copy(musicBrainzReleaseGroupId = releaseGroup.id)

        override val duration: Duration?
            get() = null
    }
}
