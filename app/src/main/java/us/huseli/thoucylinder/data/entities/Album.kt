package us.huseli.thoucylinder.data.entities

data class Album(
    val title: String,
    val artist: String? = null,
    val path: String? = null,
    val playlistId: String? = null,
    val tracks: MutableList<Track> = mutableListOf(),
    var trackCount: Int? = null,
) {
    override fun toString(): String {
        val string = artist?.let { "$it - $title" } ?: title
        return trackCount?.let { "$string ($it tracks)" } ?: string
    }
}
