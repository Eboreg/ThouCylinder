package us.huseli.thoucylinder.dataclasses.spotify

import com.google.gson.annotations.SerializedName
import org.apache.commons.text.similarity.LevenshteinDistance
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.UnsavedArtist
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.entities.Tag
import us.huseli.thoucylinder.dataclasses.musicBrainz.capitalizeGenreName
import us.huseli.thoucylinder.dataclasses.views.AlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.views.AlbumCombo
import us.huseli.thoucylinder.dataclasses.views.stripTitleCommons
import us.huseli.thoucylinder.interfaces.IExternalAlbum
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

abstract class AbstractSpotifyAlbum : AbstractSpotifyItem() {
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

    val artistName: String
        get() = artists.artistString()

    val year: Int?
        get() = releaseDate.substringBefore('-').toIntOrNull()

    fun matchAlbumCombo(albumCombo: AbstractAlbumCombo) =
        AlbumMatch(distance = getAlbumDistance(albumCombo), spotifyAlbum = this)

    suspend fun toAlbum(isLocal: Boolean, isInLibrary: Boolean) = Album(
        title = name,
        isInLibrary = isInLibrary,
        isLocal = isLocal,
        year = year,
        spotifyId = id,
        albumArt = images.toMediaStoreImage(),
    )

    open suspend fun toAlbumCombo(
        isLocal: Boolean,
        isInLibrary: Boolean,
        getArtist: suspend (UnsavedArtist) -> Artist,
    ): AlbumCombo {
        val album = toAlbum(isLocal = isLocal, isInLibrary = isInLibrary)
        val albumArtists = artists.mapIndexed { index, artist ->
            AlbumArtistCredit(
                artist = getArtist(UnsavedArtist(name = artist.name, spotifyId = artist.id)),
                albumId = album.albumId
            ).copy(spotifyId = artist.id, position = index)
        }

        return AlbumCombo(album = album, artists = albumArtists, trackCount = totalTracks ?: 0)
    }

    private fun getAlbumDistance(albumCombo: AbstractAlbumCombo): Double {
        val levenshtein = LevenshteinDistance()
        var distance = levenshtein.apply(albumCombo.album.title, name).toDouble()

        distance += artists.getDistances(albumCombo.artists).minOrNull() ?: 0

        return distance
    }

    override fun toString(): String = artistName.takeIf { it.isNotEmpty() }?.let { "$it - $name" } ?: name
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
) : AbstractSpotifyAlbum(), IExternalAlbum {
    override val duration: Duration
        get() = tracks.items.sumOf { it.durationMs }.milliseconds

    override val title: String
        get() = name

    override val playCount: Int?
        get() = null

    override val thumbnailUrl: String?
        get() = images.getThumbnailUrl()

    override val trackCount: Int?
        get() = totalTracks

    override suspend fun getMediaStoreImage(): MediaStoreImage? = images.toMediaStoreImage()

    override suspend fun toAlbumWithTracks(
        isLocal: Boolean,
        isInLibrary: Boolean,
        getArtist: suspend (UnsavedArtist) -> Artist,
    ): AlbumWithTracksCombo {
        val albumCombo = toAlbumCombo(
            getArtist = getArtist,
            isLocal = isLocal,
            isInLibrary = isInLibrary,
        )

        return AlbumWithTracksCombo(
            album = albumCombo.album,
            artists = albumCombo.artists,
            tags = genres.map { Tag(name = capitalizeGenreName(it)) },
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
        getArtist: suspend (UnsavedArtist) -> Artist,
    ): AlbumCombo = super.toAlbumCombo(
        getArtist = getArtist,
        isLocal = isLocal,
        isInLibrary = isInLibrary,
    ).copy(trackCount = tracks.total)
}


data class SpotifySavedAlbumObject(
    @SerializedName("added_at")
    val addedAt: String,
    val album: SpotifyAlbum,
)
