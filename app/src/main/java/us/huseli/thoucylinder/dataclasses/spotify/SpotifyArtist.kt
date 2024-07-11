package us.huseli.thoucylinder.dataclasses.spotify

import org.apache.commons.text.similarity.LevenshteinDistance
import us.huseli.thoucylinder.dataclasses.artist.UnsavedAlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.UnsavedArtist
import us.huseli.thoucylinder.dataclasses.artist.UnsavedTrackArtistCredit

abstract class AbstractSpotifyArtist : AbstractSpotifyItem() {
    abstract val href: String?
    abstract override val id: String
    abstract val name: String
    abstract val uri: String?
}

data class SpotifySimplifiedArtist(
    override val href: String?,
    override val id: String,
    override val name: String,
    override val uri: String?,
) : AbstractSpotifyArtist()

data class SpotifyArtist(
    override val href: String,
    override val id: String,
    override val name: String,
    override val uri: String?,
    val images: List<SpotifyImage>,
    val genres: List<String>,
    val popularity: Int?,
    val followers: Followers,
) : AbstractSpotifyArtist() {
    data class Followers(val total: Int)

    fun toNativeArtist() = UnsavedArtist(
        name = name,
        spotifyId = id,
        image = images.toMediaStoreImage(),
    )
}

fun Iterable<SpotifyArtist>.toNativeArtists(): List<UnsavedArtist> = map { it.toNativeArtist() }

fun Iterable<AbstractSpotifyArtist>.artistString() = joinToString("/") { it.name }

fun Iterable<AbstractSpotifyArtist>.getDistances(artistNames: Iterable<String>): List<Int> {
    val levenshtein = LevenshteinDistance()

    return artistNames.flatMap { name ->
        filter { it.name.lowercase() != "various artists" }.map { spotifyArtist ->
            levenshtein.apply(name, spotifyArtist.name)
        }
    }
}

fun Iterable<AbstractSpotifyArtist>.toNativeAlbumArtists(albumId: String) =
    filter { it.name.lowercase() != "various artists" }
        .mapIndexed { index, artist ->
            UnsavedAlbumArtistCredit(
                name = artist.name,
                spotifyId = artist.id,
                albumId = albumId,
                position = index,
            )
        }

fun Iterable<AbstractSpotifyArtist>.toNativeTrackArtists(trackId: String) =
    filter { it.name.lowercase() != "various artists" }
        .mapIndexed { index, artist ->
            UnsavedTrackArtistCredit(
                name = artist.name,
                spotifyId = artist.id,
                trackId = trackId,
                position = index,
            )
        }
