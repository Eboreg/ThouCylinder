package us.huseli.thoucylinder.dataclasses

data class DiscogsMaster(val data: Data) {
    data class Data(
        val title: String,
        val year: Int? = null,
        val id: String,
        val artists: List<Artist>,
        val tracklist: List<Track>,
        val styles: List<String>? = emptyList(),
        val genres: List<String>? = emptyList(),
        val images: List<Image> = emptyList(),
    )

    data class Image(
        val type: String,
        val uri: String,
        val width: Int,
        val height: Int,
    )

    data class Track(
        val position: String,
        val title: String,
        val artists: List<Artist>? = null,
        val year: Int? = null,
    )

    data class Artist(val name: String)
}
