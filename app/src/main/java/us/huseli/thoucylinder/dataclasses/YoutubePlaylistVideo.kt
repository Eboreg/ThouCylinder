package us.huseli.thoucylinder.dataclasses

data class YoutubePlaylistVideo(
    val id: String,
    val playlistId: String,
    val position: Int,
    var video: YoutubeVideo,
)
