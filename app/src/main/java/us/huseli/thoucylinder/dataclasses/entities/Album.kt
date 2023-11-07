package us.huseli.thoucylinder.dataclasses.entities

import android.content.Context
import android.graphics.Bitmap
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import us.huseli.retaintheme.sanitizeFilename
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.YoutubePlaylist
import java.util.UUID

@Entity(
    indices = [Index("Album_isInLibrary")],
)
@Parcelize
data class Album(
    @PrimaryKey @ColumnInfo("Album_albumId") val albumId: UUID = UUID.randomUUID(),
    @ColumnInfo("Album_title") val title: String,
    @ColumnInfo("Album_isInLibrary") val isInLibrary: Boolean,
    @ColumnInfo("Album_isLocal") val isLocal: Boolean,
    @ColumnInfo("Album_artist") val artist: String? = null,
    @ColumnInfo("Album_year") val year: Int? = null,
    @Embedded("Album_youtubePlaylist_") val youtubePlaylist: YoutubePlaylist? = null,
    @Embedded("Album_albumArt_") val albumArt: MediaStoreImage? = null,
) : Parcelable {
    val isOnYoutube: Boolean
        get() = youtubePlaylist != null

    suspend fun getFullImage(context: Context): Bitmap? =
        albumArt?.getBitmap(context) ?: youtubePlaylist?.getBitmap()

    fun getMediaStoreSubdir(): String =
        artist?.let { "${artist.sanitizeFilename()}/${title.sanitizeFilename()}" } ?: title.sanitizeFilename()

    suspend fun getThumbnail(context: Context): Bitmap? =
        albumArt?.getThumbnailBitmap(context) ?: youtubePlaylist?.getBitmap()

    override fun toString(): String = artist?.let { "$it - $title" } ?: title
}
