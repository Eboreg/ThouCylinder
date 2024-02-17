package us.huseli.thoucylinder.dataclasses.spotify

data class SpotifyArtist(
    val href: String?,
    val id: String,
    val name: String,
    val uri: String?,
)

fun Collection<SpotifyArtist>.artistString() = joinToString("/") { it.name }
