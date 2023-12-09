package us.huseli.thoucylinder.dataclasses.entities

import android.content.Context
import android.os.Parcelable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import org.apache.commons.text.similarity.LevenshteinDistance
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

    suspend fun getFullImage(context: Context): ImageBitmap? =
        albumArt?.getImageBitmap(context) ?: youtubePlaylist?.getImageBitmap()

    fun getLevenshteinDistance(other: Album): Int {
        val levenshtein = LevenshteinDistance()
        val distances = mutableListOf<Int>()

        distances.add(levenshtein.apply(title.lowercase(), other.title.lowercase()))
        if (artist != null) {
            distances.add(levenshtein.apply("$artist - $title".lowercase(), other.title.lowercase()))
            if (other.artist != null) distances.add(
                levenshtein.apply(
                    "$artist - $title".lowercase(),
                    "${other.artist} - ${other.title}".lowercase()
                )
            )
        }
        if (other.artist != null)
            distances.add(levenshtein.apply(title.lowercase(), "${other.artist} - ${other.title}".lowercase()))

        return distances.min()
    }

    fun getSubdir(): String =
        artist?.let { "${artist.sanitizeFilename()}/${title.sanitizeFilename()}" } ?: title.sanitizeFilename()

    suspend fun getThumbnail(context: Context): ImageBitmap? =
        albumArt?.getThumbnailImageBitmap(context) ?: youtubePlaylist?.getImageBitmap()

    override fun toString(): String = artist?.let { "$it - $title" } ?: title
}
