package us.huseli.thoucylinder.data.entities

data class YoutubePlaylist(
    val title: String,
    val id: String,
    val subtitle: String? = null,
    var videos: List<YoutubeVideo> = emptyList(),
    val videoIds: List<String> = emptyList(),
    var thumbnail: YoutubeThumbnail? = null,
) {
    override fun toString(): String {
        return "${subtitle?.let { "$title ($it)" } ?: title} (${videos.size} videos)"
    }
}
