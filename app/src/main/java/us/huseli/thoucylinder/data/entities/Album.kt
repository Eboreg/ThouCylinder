package us.huseli.thoucylinder.data.entities

import java.util.UUID

data class Album(
    val title: String,
    val id: UUID = UUID.randomUUID(),
    val artist: String? = null,
    val localPath: String? = null,
    val youtubePlaylist: YoutubePlaylist? = null,
    val tracks: List<Track> = emptyList(),
    val albumArt: Image? = null,
    val youtubeThumbnail: YoutubeThumbnail? = null,
) {
    override fun toString(): String = artist?.let { "$it - $title" } ?: title
}
