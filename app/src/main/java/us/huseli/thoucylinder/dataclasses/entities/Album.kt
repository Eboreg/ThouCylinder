package us.huseli.thoucylinder.dataclasses.entities

import android.content.ContentValues
import android.provider.MediaStore
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import us.huseli.retaintheme.sanitizeFilename
import us.huseli.thoucylinder.dataclasses.Image
import us.huseli.thoucylinder.dataclasses.YoutubePlaylist
import java.util.UUID

@Entity(
    indices = [Index("isInLibrary")],
)
data class Album(
    @PrimaryKey val albumId: UUID = UUID.randomUUID(),
    val title: String,
    val isInLibrary: Boolean,
    val isLocal: Boolean,
    val artist: String? = null,
    val year: Int? = null,
    @Embedded("youtubePlaylist") val youtubePlaylist: YoutubePlaylist? = null,
    @Embedded("albumArt") val albumArt: Image? = null,
) {
    val isOnYoutube: Boolean
        get() = youtubePlaylist != null

    fun getMediaStoreSubdir(): String =
        artist?.let { "${artist.sanitizeFilename()}/${title.sanitizeFilename()}" } ?: title.sanitizeFilename()

    fun getContentValues() = ContentValues().apply {
        put(MediaStore.Audio.Media.ALBUM, title)
        artist?.also {
            put(MediaStore.Audio.Media.ARTIST, it)
            put(MediaStore.Audio.Media.ALBUM_ARTIST, it)
        }
        year?.also { put(MediaStore.Audio.Media.YEAR, it) }
    }

    override fun toString(): String = artist?.let { "$it - $title" } ?: title
}
