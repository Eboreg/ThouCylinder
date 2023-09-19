package us.huseli.thoucylinder.dataclasses

import androidx.room.Ignore
import us.huseli.thoucylinder.sanitizeFilename
import java.util.UUID

data class YoutubePlaylist(
    val id: String,
    val title: String,
    @Ignore val artist: String? = null,
    @Ignore val thumbnail: Image? = null,
    @Ignore val videoCount: Int = 0,
) {
    constructor(id: String, title: String) : this(id, title, null, null, 0)

    fun generateSubdirName(): String =
        "${artist?.let { "$it - " } ?: ""}$title".sanitizeFilename()

    fun toAlbum(albumId: UUID = UUID.randomUUID(), tracks: List<Track> = emptyList()): Album {
        return Album(
            id = albumId,
            title = title,
            artist = artist,
            localPath = generateSubdirName(),
            youtubePlaylist = this,
            tracks = tracks,
            albumArt = thumbnail,
        )
    }

    override fun toString(): String {
        return "${artist?.let { "$it - $title" } ?: title} ($videoCount videos)"
    }
}
