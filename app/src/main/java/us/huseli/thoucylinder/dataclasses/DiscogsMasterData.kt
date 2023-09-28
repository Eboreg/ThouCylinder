package us.huseli.thoucylinder.dataclasses

data class DiscogsArtist(val name: String)

data class DiscogsMasterTrack(
    val position: String,
    val title: String,
    val artists: List<DiscogsArtist>? = null,
    val year: Int? = null,
) {
    val artist: String?
        get() = artists?.joinToString("/") { it.name }
}


data class DiscogsMasterImage(
    val type: String,
    val uri: String,
    val width: Int,
    val height: Int,
)


data class DiscogsMasterData(
    val title: String,
    val year: Int? = null,
    val id: Int,
    val artists: List<DiscogsArtist>,
    val tracklist: List<DiscogsMasterTrack>,
    val styles: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val images: List<DiscogsMasterImage> = emptyList(),
) {
    val artist: String
        get() = artists.joinToString("/") { it.name }
}


data class DiscogsMaster(val data: DiscogsMasterData)
