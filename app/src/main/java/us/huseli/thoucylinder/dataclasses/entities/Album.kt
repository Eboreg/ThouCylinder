package us.huseli.thoucylinder.dataclasses.entities

import android.content.ContentValues
import android.provider.MediaStore
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import us.huseli.retaintheme.sanitizeFilename
import us.huseli.thoucylinder.dataclasses.Image
import us.huseli.thoucylinder.dataclasses.YoutubePlaylist
import java.util.UUID

@Entity(
    indices = [Index("Album_isInLibrary")],
)
data class Album(
    @PrimaryKey @ColumnInfo("Album_albumId") val albumId: UUID = UUID.randomUUID(),
    @ColumnInfo("Album_title") val title: String,
    @ColumnInfo("Album_isInLibrary") val isInLibrary: Boolean,
    @ColumnInfo("Album_isLocal") val isLocal: Boolean,
    @ColumnInfo("Album_artist") val artist: String? = null,
    @ColumnInfo("Album_year") val year: Int? = null,
    @Embedded("Album_youtubePlaylist_") val youtubePlaylist: YoutubePlaylist? = null,
    @Embedded("Album_albumArt_") val albumArt: Image? = null,
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
