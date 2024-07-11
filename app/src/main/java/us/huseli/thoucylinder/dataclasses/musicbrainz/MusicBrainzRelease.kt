package us.huseli.thoucylinder.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.album.IUnsavedAlbumCombo
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbum
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbumCombo
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbumWithTracksCombo
import us.huseli.thoucylinder.enums.AlbumType
import us.huseli.thoucylinder.interfaces.IExternalAlbum
import us.huseli.thoucylinder.interfaces.IExternalAlbumWithTracks
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Suppress("unused")
enum class MusicBrainzReleaseStatus {
    @SerializedName("Official") OFFICIAL,
    @SerializedName("Promotion") PROMOTION,
    @SerializedName("Bootleg") BOOTLEG,
    @SerializedName("Pseudo-release") PSEUDO_RELEASE,
    @SerializedName("Withdrawn") WITHDRAWN,
    @SerializedName("Cancelled") CANCELLED,
}

abstract class AbstractMusicBrainzRelease : AbstractMusicBrainzItem(), IExternalAlbum {
    abstract val artistCredit: List<MusicBrainzArtistCredit>
    abstract val country: String?
    abstract val date: String?
    abstract val status: MusicBrainzReleaseStatus?

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
    val disambiguation: String?,
    val genres: List<MusicBrainzGenre>,
    override val id: String,
    val packaging: String?,
    @SerializedName("packaging-id")
    val packagingId: String?,
    val quality: String?,
    override val status: MusicBrainzReleaseStatus?,
    @SerializedName("status-id")
    val statusId: String?,
    override val title: String,
) : AbstractMusicBrainzRelease() {
    override val trackCount: Int?
        get() = null
    override val duration: Duration?
        get() = null
    override val year: Int?
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
    @SerializedName("packaging-id")
    val packagingId: String?,
    val quality: String?,
    @SerializedName("release-group")
    val releaseGroup: MusicBrainzSimplifiedReleaseGroup,
    override val status: MusicBrainzReleaseStatus?,
    @SerializedName("status-id")
    val statusId: String?,
    override val title: String,
) : AbstractMusicBrainzRelease(), IExternalAlbumWithTracks {
    private val allGenres: List<MusicBrainzGenre>
        get() = genres
            .asSequence()
            .plus(releaseGroup.genres)
            .plus(media.flatMap { media -> media.tracks.flatMap { track -> track.recording.genres } })
            .groupBy { it }
            .map { (genre, instances) -> genre.copy(count = instances.sumOf { it.count }) }
            .sortedByDescending { it.count }

    override val albumType: AlbumType?
        get() = super.albumType ?: releaseGroup.primaryType?.albumType

    override val duration: Duration
        get() = media.sumOf { medium -> medium.tracks.sumOf { it.length } }.milliseconds

    override val trackCount: Int
        get() = media.sumOf { it.trackCount }

    override val year: Int?
        get() = releaseGroup.year ?: date
            ?.substringBefore('-')
            ?.takeIf { it.matches(Regex("^\\d{4}$")) }
            ?.toInt()

    override fun toAlbum(isLocal: Boolean, isInLibrary: Boolean, albumId: String?): UnsavedAlbum =
        super.toAlbum(isLocal, isInLibrary, albumId).copy(musicBrainzReleaseGroupId = releaseGroup.id)

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
    ): UnsavedAlbumWithTracksCombo {
        val album = toAlbum(isLocal = isLocal, isInLibrary = isInLibrary, albumId = albumId).copy(albumArt = albumArt)
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
