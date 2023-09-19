package us.huseli.thoucylinder.dataclasses

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class Album(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val title: String,
    val artist: String? = null,
    val localPath: String? = null,
    @Embedded("youtubePlaylist") val youtubePlaylist: YoutubePlaylist? = null,
    @Embedded("albumArt") val albumArt: Image? = null,
    @Ignore val tracks: List<Track> = emptyList(),
) {
    constructor(
        id: UUID,
        title: String,
        artist: String? = null,
        localPath: String? = null,
        youtubePlaylist: YoutubePlaylist? = null,
        albumArt: Image? = null,
    ) : this(id, title, artist, localPath, youtubePlaylist, albumArt, emptyList())

    override fun toString(): String = artist?.let { "$it - $title" } ?: title
}
