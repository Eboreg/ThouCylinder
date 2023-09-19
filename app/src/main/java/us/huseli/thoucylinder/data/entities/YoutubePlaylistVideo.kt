package us.huseli.thoucylinder.data.entities

data class YoutubePlaylistVideo(
    val id: String,
    val playlistId: String,
    val position: Int,
    var video: YoutubeVideo,
)
