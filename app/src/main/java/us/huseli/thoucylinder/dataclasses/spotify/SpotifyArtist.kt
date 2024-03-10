package us.huseli.thoucylinder.dataclasses.spotify

import org.apache.commons.text.similarity.LevenshteinDistance
import us.huseli.thoucylinder.dataclasses.entities.Artist

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
}

data class SpotifyTopArtistMatch(
    val artists: Set<String>,
    val spotifyArtist: SpotifyArtist,
    val score: Int,
)

fun Collection<AbstractSpotifyArtist>.artistString() = joinToString("/") { it.name }

fun Iterable<AbstractSpotifyArtist>.getDistances(artists: Iterable<Artist>): List<Int> {
    val levenshtein = LevenshteinDistance()

    return artists.flatMap { artist ->
        map { spotifyArtist ->
            levenshtein.apply(artist.name, spotifyArtist.name)
        }
    }
}
