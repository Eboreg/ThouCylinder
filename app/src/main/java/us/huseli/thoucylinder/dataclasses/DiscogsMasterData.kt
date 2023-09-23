package us.huseli.thoucylinder.dataclasses

data class DiscogsArtist(val name: String)

data class DiscogsMasterTrack(
    val position: String,
    val title: String,
    val artists: List<DiscogsArtist>? = null,
) {
    val artist: String?
        get() = artists?.joinToString("/") { it.name }
}

data class DiscogsMasterData(
    val title: String,
    val year: Int,
    val id: Int,
    val artists: List<DiscogsArtist>,
    val tracklist: List<DiscogsMasterTrack>,
) {
    val artist: String
        get() = artists.joinToString("/") { it.name }
}


data class DiscogsMaster(val data: DiscogsMasterData)
