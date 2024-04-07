package us.huseli.thoucylinder.dataclasses.spotify

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import com.google.gson.annotations.SerializedName
import org.apache.commons.text.similarity.LevenshteinDistance
import us.huseli.retaintheme.extensions.capitalized
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.UnsavedArtist
import us.huseli.thoucylinder.dataclasses.views.AlbumCombo
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.views.stripTitleCommons
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.entities.Tag
import us.huseli.thoucylinder.dataclasses.views.AlbumArtistCredit
import us.huseli.thoucylinder.interfaces.IExternalAlbum
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

    data class AlbumMatch(
        val distance: Double,
        val spotifyAlbum: AbstractSpotifyAlbum,
    )

    override val title: String
        get() = name

    override val artistName: String
        get() = artists.artistString()

    val year: Int?
        get() = releaseDate.substringBefore('-').toIntOrNull()

    override suspend fun getThumbnailImageBitmap(context: Context): ImageBitmap? =
        images.getThumbnailImageBitmap(context)

    fun matchAlbumCombo(albumCombo: AbstractAlbumCombo) =
        AlbumMatch(distance = getAlbumDistance(albumCombo), spotifyAlbum = this)

    fun toAlbum(isLocal: Boolean, isInLibrary: Boolean, albumArt: MediaStoreImage? = null) = Album(
        title = name,
        isInLibrary = isInLibrary,
        isLocal = isLocal,
        year = year,
        spotifyId = id,
        albumArt = albumArt ?: images.toMediaStoreImage(),
    )

    open suspend fun toAlbumCombo(
        isLocal: Boolean,
        isInLibrary: Boolean,
        albumArt: MediaStoreImage? = null,
        getArtist: suspend (UnsavedArtist) -> Artist,
    ): AlbumCombo {
        val album = toAlbum(isLocal = isLocal, isInLibrary = isInLibrary, albumArt = albumArt)
        val albumArtists = artists.mapIndexed { index, artist ->
            val unsavedArtist = UnsavedArtist(name = artist.name, spotifyId = artist.id)
            AlbumArtistCredit(artist = getArtist(unsavedArtist), albumId = album.albumId)
                .copy(spotifyId = artist.id, position = index)
        }

        return AlbumCombo(album = album, artists = albumArtists, trackCount = totalTracks ?: 0)
    }

    private fun getAlbumDistance(albumCombo: AbstractAlbumCombo): Double {
        val levenshtein = LevenshteinDistance()
        var distance = levenshtein.apply(albumCombo.album.title, name).toDouble()

        distance += artists.getDistances(albumCombo.artists).minOrNull() ?: 0

        return distance
    }

    override fun toString(): String = artistName.takeIf { it.isNotEmpty() }?.let { "$it - $title" } ?: title
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
        isLocal: Boolean,
        isInLibrary: Boolean,
        albumArt: MediaStoreImage? = null,
        getArtist: suspend (UnsavedArtist) -> Artist,
    ): AlbumWithTracksCombo {
        val albumCombo = toAlbumCombo(
            getArtist = getArtist,
            isLocal = isLocal,
            isInLibrary = isInLibrary,
            albumArt = albumArt,
        )

        return AlbumWithTracksCombo(
            album = albumCombo.album,
            artists = albumCombo.artists,
            tags = genres.map { Tag(name = it.capitalized()) },
            trackCombos = tracks.items.map {
                it.toTrackCombo(
                    getArtist = getArtist,
                    album = albumCombo.album,
                    isLocal = isLocal,
                    isInLibrary = isInLibrary,
                )
            }.stripTitleCommons(),
        )
    }

    override suspend fun toAlbumCombo(
        isLocal: Boolean,
        isInLibrary: Boolean,
        albumArt: MediaStoreImage?,
        getArtist: suspend (UnsavedArtist) -> Artist,
    ): AlbumCombo = super.toAlbumCombo(
        getArtist = getArtist,
        isLocal = isLocal,
        isInLibrary = isInLibrary,
        albumArt = albumArt,
    ).copy(trackCount = tracks.total)
}


fun List<SpotifyAlbum?>.filterBySearchTerm(term: String): List<SpotifyAlbum> {
    val words = term.lowercase().split(Regex(" +"))

    return filterNotNull().filter { album ->
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
