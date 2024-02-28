package us.huseli.thoucylinder.dataclasses.spotify

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
    val artists: List<String>,
    val spotifyArtist: SpotifyArtist,
    val score: Int,
)

fun Collection<AbstractSpotifyArtist>.artistString() = joinToString("/") { it.name }
