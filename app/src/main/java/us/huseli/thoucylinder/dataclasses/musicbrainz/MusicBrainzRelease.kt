package us.huseli.thoucylinder.dataclasses.musicbrainz

import android.content.Context
import com.google.gson.annotations.SerializedName
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.album.IUnsavedAlbumCombo
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbum
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbumCombo
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbumWithTracksCombo
import us.huseli.thoucylinder.enums.AlbumType
import us.huseli.thoucylinder.enums.getRegionName
import us.huseli.thoucylinder.interfaces.IExternalAlbum
import us.huseli.thoucylinder.interfaces.IExternalAlbumWithTracks
import us.huseli.thoucylinder.interfaces.IHasMusicBrainzIds
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Suppress("unused", "IncorrectFormatting")
enum class MusicBrainzReleaseStatus {
    @SerializedName("Official") OFFICIAL,
    @SerializedName("Promotion") PROMOTION,
    @SerializedName("Bootleg") BOOTLEG,
    @SerializedName("Pseudo-release") PSEUDO_RELEASE,
    @SerializedName("Withdrawn") WITHDRAWN,
    @SerializedName("Cancelled") CANCELLED,
}

abstract class AbstractMusicBrainzRelease : AbstractMusicBrainzItem(), IExternalAlbum, IHasMusicBrainzIds {
    abstract val artistCredit: List<MusicBrainzArtistCredit>
    abstract val country: String?
    abstract val date: String?
    abstract val status: MusicBrainzReleaseStatus?
    abstract val releaseGroupId: String?

    override val albumType: AlbumType?
        get() = if (artistCredit.map { it.name.lowercase() }.contains("various artists"))
            AlbumType.COMPILATION
        else null

    override val artistName: String
        get() = artistCredit.joined()

    override val playCount: Int?
        get() = null

    override val thumbnailUrl: String?
        get() = null

    override val year: Int?
        get() = date
            ?.substringBefore('-')
            ?.takeIf { it.matches(Regex("^\\d{4}$")) }
            ?.toInt()

    override val musicBrainzReleaseId: String?
        get() = id

    override val musicBrainzReleaseGroupId: String?
        get() = releaseGroupId

    fun getCountryName(context: Context): String? = country?.let { context.getRegionName(it) }

    open fun toAlbum(
        isLocal: Boolean,
        isInLibrary: Boolean,
        albumId: String? = null,
    ) = UnsavedAlbum(
        albumId = albumId ?: UUID.randomUUID().toString(),
        albumType = albumType,
        isInLibrary = isInLibrary,
        isLocal = isLocal,
        musicBrainzReleaseId = id,
        title = title,
        trackCount = trackCount,
        year = year,
    )

    override fun toAlbumCombo(
        isLocal: Boolean,
        isInLibrary: Boolean,
        albumId: String?,
    ): IUnsavedAlbumCombo {
        val album = toAlbum(isLocal = isLocal, isInLibrary = isInLibrary, albumId = albumId)
        val albumArtists = artistCredit.toNativeAlbumArtists(albumId = album.albumId)

        return UnsavedAlbumCombo(album = album, artists = albumArtists)
    }
}

data class MusicBrainzSimplifiedRelease(
    @SerializedName("artist-credit")
    override val artistCredit: List<MusicBrainzArtistCredit>,
    override val country: String?,
    override val date: String?,
    val disambiguation: String,
    val genres: List<MusicBrainzGenre>,
    override val id: String,
    val packaging: String?,
    override val status: MusicBrainzReleaseStatus?,
    override val title: String,
) : AbstractMusicBrainzRelease() {
    override val trackCount: Int?
        get() = null

    override val duration: Duration?
        get() = null

    override val year: Int?
        get() = null

    override val releaseGroupId: String?
        get() = null

    override fun toAlbumCombo(
        isLocal: Boolean,
        isInLibrary: Boolean,
        albumId: String?,
    ): UnsavedAlbumCombo {
        val album = toAlbum(isLocal = isLocal, isInLibrary = isInLibrary, albumId = albumId)
        val albumArtists = artistCredit.toNativeAlbumArtists(albumId = album.albumId)

        return UnsavedAlbumCombo(album = album, artists = albumArtists)
    }
}

data class MusicBrainzRelease(
    @SerializedName("artist-credit")
    override val artistCredit: List<MusicBrainzArtistCredit>,
    override val country: String?,
    override val date: String?,
    val genres: List<MusicBrainzGenre>,
    override val id: String,
    val media: List<MusicBrainzMedia>,
    val packaging: String?,
    @SerializedName("release-group")
    val releaseGroup: MusicBrainzSimplifiedReleaseGroup?,
    override val status: MusicBrainzReleaseStatus?,
    override val title: String,
    @SerializedName("label-info")
    val labelInfo: List<MusicBrainzLabelInfo>?,
) : AbstractMusicBrainzRelease(), IExternalAlbumWithTracks {
    private val allGenres: List<MusicBrainzGenre>
        get() = genres
            .asSequence()
            .plus(releaseGroup?.genres ?: emptyList())
            .plus(media.flatMap { media -> media.tracks.flatMap { track -> track.recording.genres } })
            .groupBy { it }
            .map { (genre, instances) -> genre.copy(count = instances.sumOf { it.count }) }
            .sortedByDescending { it.count }

    override val albumType: AlbumType?
        get() = super.albumType ?: releaseGroup?.albumType

    override val duration: Duration
        get() = media.sumOf { medium -> medium.tracks.sumOf { it.length } }.milliseconds

    override val trackCount: Int
        get() = media.sumOf { it.trackCount }

    override val releaseGroupId: String?
        get() = releaseGroup?.id

    override val year: Int?
        get() = releaseGroup?.year ?: date
            ?.substringBefore('-')
            ?.takeIf { it.matches(Regex("^\\d{4}$")) }
            ?.toInt()

    override fun toAlbum(isLocal: Boolean, isInLibrary: Boolean, albumId: String?): UnsavedAlbum =
        super.toAlbum(isLocal, isInLibrary, albumId).copy(musicBrainzReleaseGroupId = releaseGroup?.id)

    override fun toAlbumCombo(
        isLocal: Boolean,
        isInLibrary: Boolean,
        albumId: String?,
    ) = toAlbumWithTracks(isLocal = isLocal, isInLibrary = isInLibrary, albumId = albumId)

    override fun toAlbumWithTracks(
        isLocal: Boolean,
        isInLibrary: Boolean,
        albumId: String?,
    ): UnsavedAlbumWithTracksCombo = toAlbumWithTracks(
        isInLibrary = isInLibrary,
        isLocal = isLocal,
        albumArt = null,
        albumId = albumId,
    )

    fun toAlbumWithTracks(
        isInLibrary: Boolean,
        isLocal: Boolean,
        albumArt: MediaStoreImage? = null,
        albumId: String? = null,
        releaseGroup: AbstractMusicBrainzReleaseGroup? = null,
    ): UnsavedAlbumWithTracksCombo {
        val album = toAlbum(isLocal = isLocal, isInLibrary = isInLibrary, albumId = albumId).let {
            it.copy(
                albumArt = albumArt,
                year = releaseGroup?.year ?: it.year,
                musicBrainzReleaseGroupId = releaseGroup?.id ?: it.musicBrainzReleaseGroupId,
            )
        }
        val albumArtists = artistCredit.toNativeAlbumArtists(albumId = album.albumId)

        return UnsavedAlbumWithTracksCombo(
            album = album,
            tags = allGenres.toInternal(),
            artists = albumArtists,
            trackCombos = media.toTrackCombos(
                isInLibrary = isInLibrary,
                album = album,
            ),
        )
    }
}
