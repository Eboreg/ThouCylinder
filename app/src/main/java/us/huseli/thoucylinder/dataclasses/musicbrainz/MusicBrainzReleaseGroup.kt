package us.huseli.thoucylinder.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbum
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbumCombo
import us.huseli.thoucylinder.enums.AlbumType
import us.huseli.thoucylinder.interfaces.IExternalAlbum
import us.huseli.thoucylinder.interfaces.IHasMusicBrainzIds
import java.util.UUID
import kotlin.time.Duration

@Suppress("unused", "IncorrectFormatting")
enum class MusicBrainzReleaseGroupPrimaryType(val sortPrio: Int) {
    @SerializedName("Album") ALBUM(0),
    @SerializedName("Single") SINGLE(2),
    @SerializedName("EP") EP(1),
    @SerializedName("Broadcast") BROADCAST(3),
    @SerializedName("Other") OTHER(4);

    val albumType: AlbumType?
        get() = when (this) {
            ALBUM -> AlbumType.ALBUM
            SINGLE -> AlbumType.SINGLE
            EP -> AlbumType.EP
            else -> null
        }
}

@Suppress("unused", "IncorrectFormatting")
enum class MusicBrainzReleaseGroupSecondaryType {
    @SerializedName("Compilation") COMPILATION,
    @SerializedName("Soundtrack") SOUNDTRACK,
    @SerializedName("Spokenword") SPOKENWORD,
    @SerializedName("Interview") INTERVIEW,
    @SerializedName("Audiobook") AUDIOBOOK,
    @SerializedName("Audio drama") AUDIO_DRAMA,
    @SerializedName("Live") LIVE,
    @SerializedName("Remix") REMIX,
    @SerializedName("DJ-mix") DJ_MIX,
    @SerializedName("Mixtape/Street") MIXTAPE_STREET,
    @SerializedName("Demo") DEMO,
    @SerializedName("Field recording") FIELD_RECORDING,
}

abstract class AbstractMusicBrainzReleaseGroup : AbstractMusicBrainzItem(), IHasMusicBrainzIds, IExternalAlbum {
    abstract val firstReleaseDate: String?
    abstract val primaryType: MusicBrainzReleaseGroupPrimaryType?
    abstract override val title: String
    abstract val secondaryTypes: List<MusicBrainzReleaseGroupSecondaryType>?
    abstract val artistCredit: List<MusicBrainzArtistCredit>?

    abstract override val id: String

    override val musicBrainzReleaseGroupId: String?
        get() = id

    override val musicBrainzReleaseId: String?
        get() = null

    override val duration: Duration?
        get() = null

    override val playCount: Int?
        get() = null

    override val thumbnailUrl: String?
        get() = null

    override val trackCount: Int?
        get() = null

    override val albumType: AlbumType?
        get() {
            if (secondaryTypes?.contains(MusicBrainzReleaseGroupSecondaryType.COMPILATION) == true)
                return AlbumType.COMPILATION
            return primaryType?.albumType
        }

    override val artistName: String?
        get() = artistCredit?.joined()

    override val year: Int?
        get() = firstReleaseDate
            ?.substringBefore('-')
            ?.takeIf { it.matches(Regex("^\\d{4}$")) }
            ?.toInt()

    override fun toAlbumCombo(isLocal: Boolean, isInLibrary: Boolean, albumId: String?): UnsavedAlbumCombo {
        val album = UnsavedAlbum(
            albumId = albumId ?: UUID.randomUUID().toString(),
            isInLibrary = isInLibrary,
            isLocal = isLocal,
            title = title,
            albumType = albumType,
            musicBrainzReleaseGroupId = id,
            year = year,
        )

        return UnsavedAlbumCombo(
            album = album,
            artists = artistCredit?.toNativeAlbumArtists(albumId = album.albumId) ?: emptyList(),
        )
    }
}

data class MusicBrainzSimplifiedReleaseGroup(
    @SerializedName("artist-credit")
    override val artistCredit: List<MusicBrainzArtistCredit>,
    @SerializedName("first-release-date")
    override val firstReleaseDate: String?,
    val genres: List<MusicBrainzGenre>,
    override val id: String,
    @SerializedName("primary-type")
    override val primaryType: MusicBrainzReleaseGroupPrimaryType?,
    override val title: String,
    @SerializedName("secondary-types")
    override val secondaryTypes: List<MusicBrainzReleaseGroupSecondaryType>,
) : AbstractMusicBrainzReleaseGroup()

data class MusicBrainzReleaseGroup(
    @SerializedName("artist-credit")
    override val artistCredit: List<MusicBrainzArtistCredit>,
    @SerializedName("first-release-date")
    override val firstReleaseDate: String?,
    val genres: List<MusicBrainzGenre>,
    override val id: String,
    @SerializedName("primary-type")
    override val primaryType: MusicBrainzReleaseGroupPrimaryType?,
    val releases: List<MusicBrainzSimplifiedRelease>,
    override val title: String,
    @SerializedName("secondary-types")
    override val secondaryTypes: List<MusicBrainzReleaseGroupSecondaryType>,
) : AbstractMusicBrainzReleaseGroup()
