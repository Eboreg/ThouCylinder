package us.huseli.thoucylinder.dataclasses

import androidx.room.Ignore
import java.util.UUID

data class YoutubePlaylist(
    val id: String,
    val title: String,
    @Ignore val artist: String? = null,
    @Ignore val thumbnail: Image? = null,
    @Ignore val videoCount: Int = 0,
) {
    constructor(id: String, title: String) : this(id, title, null, null, 0)

    override fun toString(): String {
        return "${artist?.let { "$it - $title" } ?: title} ($videoCount videos)"
    }

    fun toTempAlbum(videos: List<YoutubeVideo>): Album {
        val albumId = UUID.randomUUID()
        return Album(
            albumId = albumId,
            title = title,
            artist = artist,
            isInLibrary = false,
            youtubePlaylist = this,
            albumArt = thumbnail,
            tracks = videos.map { it.toTrack(albumId = albumId, isInLibrary = false) },
        )
    }
}
