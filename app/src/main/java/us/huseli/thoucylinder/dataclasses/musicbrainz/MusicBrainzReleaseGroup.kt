package us.huseli.thoucylinder.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName
import us.huseli.thoucylinder.enums.AlbumType

@Suppress("unused")
enum class MusicBrainzReleaseGroupPrimaryType {
    @SerializedName("Album") ALBUM,
    @SerializedName("Single") SINGLE,
    @SerializedName("EP") EP,
    @SerializedName("Broadcast") BROADCAST,
    @SerializedName("Other") OTHER;

    val albumType: AlbumType?
        get() = when (this) {
            ALBUM -> AlbumType.ALBUM
            SINGLE -> AlbumType.SINGLE
            EP -> AlbumType.EP
            else -> null
        }
}

abstract class AbstractMusicBrainzReleaseGroup : AbstractMusicBrainzItem() {
    abstract val artistCredit: List<MusicBrainzArtistCredit>
    abstract val firstReleaseDate: String?
    abstract val genres: List<MusicBrainzGenre>
    abstract val primaryType: MusicBrainzReleaseGroupPrimaryType?
    abstract val primaryTypeId: String?
    abstract val title: String

    val albumType: AlbumType?
        get() = primaryType?.albumType

    val year: Int?
        get() = firstReleaseDate
            ?.substringBefore('-')
            ?.takeIf { it.matches(Regex("^\\d{4}$")) }
            ?.toInt()
}

data class MusicBrainzSimplifiedReleaseGroup(
    @SerializedName("artist-credit")
    override val artistCredit: List<MusicBrainzArtistCredit>,
    val disambiguation: String?,
    @SerializedName("first-release-date")
    override val firstReleaseDate: String?,
    override val genres: List<MusicBrainzGenre>,
    override val id: String,
    @SerializedName("primary-type")
    override val primaryType: MusicBrainzReleaseGroupPrimaryType?,
    @SerializedName("primary-type-id")
    override val primaryTypeId: String?,
    override val title: String,
) : AbstractMusicBrainzReleaseGroup()

data class MusicBrainzReleaseGroup(
    @SerializedName("artist-credit")
    override val artistCredit: List<MusicBrainzArtistCredit>,
    val disambiguation: String?,
    @SerializedName("first-release-date")
    override val firstReleaseDate: String?,
    override val genres: List<MusicBrainzGenre>,
    override val id: String,
    @SerializedName("primary-type")
    override val primaryType: MusicBrainzReleaseGroupPrimaryType?,
    @SerializedName("primary-type-id")
    override val primaryTypeId: String?,
    val releases: List<MusicBrainzSimplifiedRelease>,
    override val title: String,
) : AbstractMusicBrainzReleaseGroup()
