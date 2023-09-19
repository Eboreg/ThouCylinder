package us.huseli.thoucylinder.dataclasses

data class YoutubePlaylistItem(
    val id: String,
    val videoId: String,
    val playlistId: String,
    val position: Int,
)
