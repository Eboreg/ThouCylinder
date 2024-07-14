package us.huseli.thoucylinder.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbum
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbumCombo
import us.huseli.thoucylinder.interfaces.IExternalAlbum
import java.util.UUID
import kotlin.time.Duration

data class MusicBrainzReleaseGroupSearch(
    val count: Int,
    val offset: Int,
    @SerializedName("release-groups") val releaseGroups: List<ReleaseGroup>,
) {
    data class ReleaseGroup(
        override val id: String,
        override val title: String,
        val count: Int,
        @SerializedName("first-release-date")
        override val firstReleaseDate: String?,
        @SerializedName("primary-type")
        override val primaryType: MusicBrainzReleaseGroupPrimaryType?,
        @SerializedName("artist-credit")
        override val artistCredit: List<MusicBrainzArtistCredit>,
        val releases: List<Release>,
        @SerializedName("secondary-types")
        override val secondaryTypes: List<MusicBrainzReleaseGroupSecondaryType>?,
    ) : AbstractMusicBrainzReleaseGroup(), IExternalAlbum {
        data class Release(
            override val id: String,
            val title: String,
            val status: MusicBrainzReleaseStatus? = null,
        ) : AbstractMusicBrainzItem()

        override val artistName: String
            get() = artistCredit.joined()
        override val thumbnailUrl: String?
            get() = null
        override val trackCount: Int?
            get() = null
        override val duration: Duration?
            get() = null
        override val playCount: Int?
            get() = null

        fun getPreferredReleaseId(): String? {
            return releases.firstOrNull { it.status == MusicBrainzReleaseStatus.OFFICIAL }?.id
                ?: releases.firstOrNull()?.id
        }

        override fun toAlbumCombo(
            isLocal: Boolean,
            isInLibrary: Boolean,
            albumId: String?,
        ): UnsavedAlbumCombo {
            val album = UnsavedAlbum(
                title = title,
                isInLibrary = isInLibrary,
                isLocal = isLocal,
                year = year,
                musicBrainzReleaseGroupId = id,
                albumId = albumId ?: UUID.randomUUID().toString(),
                albumType = albumType,
            )
            val albumArtists = artistCredit.toNativeAlbumArtists(albumId = album.albumId)

            return UnsavedAlbumCombo(album = album, artists = albumArtists)
        }
    }
}
