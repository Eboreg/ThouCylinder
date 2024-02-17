package us.huseli.thoucylinder.dataclasses.spotify

import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import us.huseli.retaintheme.extensions.capitalized
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Tag
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.entities.stripTitleCommons
import us.huseli.thoucylinder.fromJson
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class SpotifyResponse<T>(
    val href: String,
    val limit: Int,
    val next: String?,
    val offset: Int,
    val previous: String?,
    val total: Int,
    val items: List<T>,
)

abstract class AbstractSpotifyResponseAlbum {
    abstract val artists: List<SpotifyArtist>
    abstract val name: String
    abstract val images: List<SpotifyAlbumArt>

    fun toAlbum(isInLibrary: Boolean) = Album(
        title = name,
        isInLibrary = isInLibrary,
        isLocal = false,
        artist = artists.artistString(),
    )
}

data class SpotifySearchResponse(val albums: SpotifyResponse<Album>) {
    data class Album(
        @SerializedName("album_type")
        val albumType: String?,
        override val artists: List<SpotifyArtist>,
        val href: String?,
        val id: String,
        override val images: List<SpotifyAlbumArt>,
        override val name: String,
        @SerializedName("release_date")
        val releaseDate: String,
        @SerializedName("release_date_precision")
        val releaseDatePrecision: String?,
        @SerializedName("total_tracks")
        val totalTracks: Int?,
        val uri: String?,
    ) : AbstractSpotifyResponseAlbum()
}

data class SpotifyResponseAlbumItem(
    @SerializedName("added_at")
    val addedAt: String,
    val album: Album,
) {
    data class Album(
        @SerializedName("album_type")
        val albumType: String?,
        override val artists: List<SpotifyArtist>,
        val genres: List<String>,
        val href: String?,
        val id: String,
        override val images: List<SpotifyAlbumArt>,
        override val name: String,
        @SerializedName("release_date")
        val releaseDate: String,
        @SerializedName("release_date_precision")
        val releaseDatePrecision: String?,
        @SerializedName("total_tracks")
        val totalTracks: Int?,
        val tracks: SpotifyResponse<Track>,
        val uri: String?,
    ) : AbstractSpotifyResponseAlbum() {
        data class Track(
            @SerializedName("disc_number")
            val discNumber: Int,
            @SerializedName("duration_ms")
            val durationMs: Int,
            val href: String?,
            val id: String,
            val name: String,
            @SerializedName("track_number")
            val trackNumber: Int,
            val uri: String?,
            val artists: List<SpotifyArtist>,
        )

        val artist: String
            get() = artists.artistString()

        val duration: Duration
            get() = durationMs.milliseconds

        val durationMs: Int
            get() = tracks.items.sumOf { it.durationMs }

        val year: Int?
            get() = releaseDate.substringBefore('-').toIntOrNull()

        fun toAlbumCombo(isInLibrary: Boolean): AlbumWithTracksCombo {
            val album = Album(
                title = name,
                isLocal = false,
                isInLibrary = isInLibrary,
                artist = artists.artistString(),
                year = year,
                spotifyId = id,
            )

            return AlbumWithTracksCombo(
                album = album,
                tags = genres.map { Tag(name = it.capitalized()) },
                tracks = tracks.items.map {
                    Track(
                        isInLibrary = isInLibrary,
                        artist = it.artists.artistString(),
                        albumId = album.albumId,
                        albumPosition = it.trackNumber,
                        title = it.name,
                        discNumber = it.discNumber,
                        spotifyId = it.id,
                    )
                }.stripTitleCommons(),
            )
        }
    }
}

fun String.toSpotifyAlbumResponse(): SpotifyResponse<SpotifyResponseAlbumItem> =
    fromJson(object : TypeToken<SpotifyResponse<SpotifyResponseAlbumItem>>() {})

fun List<SpotifyResponseAlbumItem.Album>.filterBySearchTerm(term: String): List<SpotifyResponseAlbumItem.Album> {
    val words = term.lowercase().split(Regex(" +"))

    return filter { album ->
        words.all {
            album.artists.artistString().lowercase().contains(it) ||
                album.name.lowercase().contains(it) ||
                album.year?.toString()?.contains(it) == true
        }
    }
}
