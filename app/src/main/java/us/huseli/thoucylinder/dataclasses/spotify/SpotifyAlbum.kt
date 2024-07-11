package us.huseli.thoucylinder.dataclasses.spotify

import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import org.apache.commons.text.similarity.LevenshteinDistance
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbum
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbumCombo
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.musicbrainz.capitalizeGenreName
import us.huseli.thoucylinder.dataclasses.tag.Tag
import us.huseli.thoucylinder.dataclasses.track.stripTitleCommons
import us.huseli.thoucylinder.enums.AlbumType
import us.huseli.thoucylinder.interfaces.IExternalAlbum
import us.huseli.thoucylinder.interfaces.IExternalAlbumWithTracks
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

enum class SpotifyAlbumType(@StringRes val stringRes: Int) {
    @SerializedName("album")
    ALBUM(R.string.album),

    @SerializedName("single")
    SINGLE(R.string.single),

    @SerializedName("compilation")
    COMPILATION(R.string.compilation);

    val nativeAlbumType: AlbumType
        get() = when (this) {
            ALBUM -> AlbumType.ALBUM
            SINGLE -> AlbumType.SINGLE
            COMPILATION -> AlbumType.COMPILATION
        }
}

abstract class AbstractSpotifyAlbum : AbstractSpotifyItem(), IExternalAlbum {
    abstract val spotifyAlbumType: SpotifyAlbumType?
    abstract val artists: List<SpotifySimplifiedArtist>
    abstract val href: String?
    abstract override val id: String
    abstract val images: List<SpotifyImage>
    abstract val name: String
    abstract val releaseDate: String
    abstract val releaseDatePrecision: String?
    abstract val totalTracks: Int
    abstract val uri: String?

    data class AlbumMatch(
        val distance: Double,
        val spotifyAlbum: AbstractSpotifyAlbum,
    )

    override val albumType: AlbumType?
        get() = spotifyAlbumType?.nativeAlbumType

    override val artistName: String
        get() = artists.artistString()

    override val duration: Duration?
        get() = null

    override val playCount: Int?
        get() = null

    override val title: String
        get() = name

    override val thumbnailUrl: String?
        get() = images.getThumbnailUrl()

    override val trackCount: Int
        get() = totalTracks

    override val year: Int?
        get() = releaseDate.substringBefore('-').toIntOrNull()

    fun matchAlbumCombo(albumTitle: String, artistNames: Collection<String>) = AlbumMatch(
        distance = getAlbumDistance(albumTitle, artistNames),
        spotifyAlbum = this,
    )

    fun toAlbum(isLocal: Boolean, isInLibrary: Boolean, albumId: String? = null) = UnsavedAlbum(
        albumArt = images.toMediaStoreImage(),
        albumId = albumId ?: UUID.randomUUID().toString(),
        albumType = albumType,
        isInLibrary = isInLibrary,
        isLocal = isLocal,
        spotifyId = id,
        title = name,
        trackCount = trackCount,
        year = year,
    )

    private fun getAlbumDistance(albumTitle: String, artistNames: Collection<String>): Double {
        val levenshtein = LevenshteinDistance()
        var distance = levenshtein.apply(albumTitle, name).toDouble()

        distance += artists.getDistances(artistNames).minOrNull() ?: 0
        return distance
    }

    override fun toString(): String = artistName.takeIf { it.isNotEmpty() }?.let { "$it - $name" } ?: name
}


data class SpotifySimplifiedAlbum(
    @SerializedName("album_type")
    override val spotifyAlbumType: SpotifyAlbumType?,
    override val artists: List<SpotifySimplifiedArtist>,
    override val href: String?,
    override val id: String,
    override val images: List<SpotifyImage>,
    override val name: String,
    @SerializedName("release_date")
    override val releaseDate: String,
    @SerializedName("release_date_precision")
    override val releaseDatePrecision: String?,
    @SerializedName("total_tracks")
    override val totalTracks: Int,
    override val uri: String?,
) : AbstractSpotifyAlbum() {
    override fun toAlbumCombo(
        isLocal: Boolean,
        isInLibrary: Boolean,
        albumId: String?,
    ): UnsavedAlbumCombo {
        val album =
            toAlbum(isLocal = isLocal, isInLibrary = isInLibrary, albumId = albumId ?: UUID.randomUUID().toString())

        return UnsavedAlbumCombo(
            album = album,
            artists = artists.toNativeAlbumArtists(albumId = album.albumId),
        )
    }
}


data class SpotifyAlbum(
    @SerializedName("album_type")
    override val spotifyAlbumType: SpotifyAlbumType?,
    override val artists: List<SpotifySimplifiedArtist>,
    override val href: String?,
    override val id: String,
    override val images: List<SpotifyImage>,
    override val name: String,
    @SerializedName("release_date")
    override val releaseDate: String,
    @SerializedName("release_date_precision")
    override val releaseDatePrecision: String?,
    @SerializedName("total_tracks")
    override val totalTracks: Int,
    override val uri: String?,
    val genres: List<String>,
    val label: String,
    val popularity: Int,
    val tracks: SpotifyResponse<SpotifySimplifiedTrack>,
) : AbstractSpotifyAlbum(), IExternalAlbumWithTracks {
    override val duration: Duration
        get() = tracks.items.sumOf { it.durationMs }.milliseconds

    override fun getMediaStoreImage(): MediaStoreImage? = images.toMediaStoreImage()

    override fun toAlbumWithTracks(
        isLocal: Boolean,
        isInLibrary: Boolean,
        albumId: String?,
    ): UnsavedAlbumWithTracksCombo {
        val album = toAlbum(isLocal = isLocal, isInLibrary = isInLibrary, albumId = albumId)

        return UnsavedAlbumWithTracksCombo(
            album = album,
            artists = artists.toNativeAlbumArtists(albumId = album.albumId),
            tags = genres.map { Tag(name = capitalizeGenreName(it)) },
            trackCombos = tracks.items.map { track ->
                track.toTrackCombo(isInLibrary = isInLibrary, album = album)
            }.stripTitleCommons(),
        )
    }
}


data class SpotifySavedAlbumObject(
    @SerializedName("added_at")
    val addedAt: String,
    val album: SpotifyAlbum,
)
