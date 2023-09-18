package us.huseli.thoucylinder.data.entities

data class YoutubePlaylistVideo(
    val id: String,
    val playlistId: String,
    var video: YoutubeVideo,
    val position: Int,
)
