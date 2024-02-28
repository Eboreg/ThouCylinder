package us.huseli.thoucylinder.dataclasses.spotify

import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import us.huseli.retaintheme.extensions.capitalized
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.combos.AlbumCombo
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.combos.stripTitleCommons
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.entities.Tag
import us.huseli.thoucylinder.dataclasses.interfaces.IExternalAlbum
import us.huseli.thoucylinder.dataclasses.views.AlbumArtistCredit
import us.huseli.thoucylinder.fromJson
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

abstract class AbstractSpotifyAlbum : AbstractSpotifyItem(), IExternalAlbum {
    abstract val albumType: String?
    abstract val artists: List<SpotifySimplifiedArtist>
    abstract val href: String?
    abstract override val id: String
    abstract val images: List<SpotifyImage>
    abstract val name: String
    abstract val releaseDate: String
    abstract val releaseDatePrecision: String?
    abstract val totalTracks: Int?
    abstract val uri: String?

    override val title: String
        get() = name

    val artist: String
        get() = artists.artistString()

    override val artistName: String?
        get() = artist

    val year: Int?
        get() = releaseDate.substringBefore('-').toIntOrNull()

    open suspend fun toAlbumCombo(
        isLocal: Boolean = false,
        albumArt: MediaStoreImage? = null,
        getArtist: suspend (String) -> Artist,
    ): AlbumCombo {
        val album = Album(
            title = name,
            isInLibrary = true,
            isLocal = isLocal,
            year = year,
            spotifyId = id,
            albumArt = albumArt ?: images.toMediaStoreImage(),
        )
        val albumArtists = artists.mapIndexed { index, artist ->
            AlbumArtistCredit(artist = getArtist(artist.name), albumId = album.albumId)
                .copy(spotifyId = artist.id, position = index)
        }

        return AlbumCombo(album = album, artists = albumArtists, trackCount = totalTracks ?: 0)
    }
}

data class SpotifySimplifiedAlbum(
    @SerializedName("album_type")
    override val albumType: String?,
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
    override val totalTracks: Int?,
    override val uri: String?,
) : AbstractSpotifyAlbum()

data class SpotifyAlbum(
    @SerializedName("album_type")
    override val albumType: String?,
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
    override val totalTracks: Int?,
    override val uri: String?,
    val genres: List<String>,
    val label: String,
    val popularity: Int,
    val tracks: SpotifyResponse<SpotifySimplifiedTrack>,
) : AbstractSpotifyAlbum() {
    val duration: Duration
        get() = durationMs.milliseconds

    val durationMs: Int
        get() = tracks.items.sumOf { it.durationMs }

    override val title: String
        get() = name

    suspend fun toAlbumWithTracks(
        isLocal: Boolean = false,
        albumArt: MediaStoreImage? = null,
        getArtist: suspend (String) -> Artist,
    ): AlbumWithTracksCombo {
        val albumCombo = toAlbumCombo(getArtist = getArtist, isLocal = isLocal, albumArt = albumArt)

        return AlbumWithTracksCombo(
            album = albumCombo.album,
            artists = albumCombo.artists,
            tags = albumCombo.tags,
            trackCombos = tracks.items.map {
                it.toTrackCombo(getArtist = getArtist, album = albumCombo.album)
            }.stripTitleCommons(),
        )
    }

    override suspend fun toAlbumCombo(
        isLocal: Boolean,
        albumArt: MediaStoreImage?,
        getArtist: suspend (String) -> Artist,
    ): AlbumCombo {
        return super.toAlbumCombo(getArtist = getArtist, isLocal = isLocal, albumArt = albumArt).copy(
            trackCount = tracks.total,
            durationMs = tracks.items.sumOf { it.durationMs.toLong() },
            tags = genres.map { Tag(name = it.capitalized()) },
        )
    }
}

fun List<SpotifyAlbum>.filterBySearchTerm(term: String): List<SpotifyAlbum> {
    val words = term.lowercase().split(Regex(" +"))

    return filter { album ->
        words.all {
            album.artists.artistString().lowercase().contains(it) ||
                album.name.lowercase().contains(it) ||
                album.year?.toString()?.contains(it) == true
        }
    }
}

data class SpotifySavedAlbumObject(
    @SerializedName("added_at")
    val addedAt: String,
    val album: SpotifyAlbum,
)

fun String.toSpotifySavedAlbumResponse(): SpotifyResponse<SpotifySavedAlbumObject> =
    fromJson(object : TypeToken<SpotifyResponse<SpotifySavedAlbumObject>>() {})
