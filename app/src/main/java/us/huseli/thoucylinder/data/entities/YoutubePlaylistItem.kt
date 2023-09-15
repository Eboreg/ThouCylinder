package us.huseli.thoucylinder.data.entities

data class YoutubePlaylistItem(
    val id: String,
    val playlistId: String,
    val video: YoutubeVideo,
)
