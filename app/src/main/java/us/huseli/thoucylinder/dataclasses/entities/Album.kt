package us.huseli.thoucylinder.dataclasses.entities

import android.content.Context
import android.graphics.Bitmap
import android.os.Parcelable
import androidx.annotation.WorkerThread
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import org.apache.commons.text.similarity.LevenshteinDistance
import us.huseli.retaintheme.sanitizeFilename
import us.huseli.retaintheme.scaleToMaxSize
import us.huseli.thoucylinder.Constants.IMAGE_MAX_DP_FULL
import us.huseli.thoucylinder.Constants.IMAGE_MAX_DP_THUMBNAIL
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.createDirectoryIfNotExists
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.YoutubePlaylist
import us.huseli.thoucylinder.getBitmapByUrl
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
    @ColumnInfo("Album_isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo("Album_isHidden") val isHidden: Boolean = false,
    @ColumnInfo("Album_lastFmFullImageUrl") val lastFmFullImageUrl: String? = null,
    @ColumnInfo("Album_lastFmThumbnailUrl") val lastFmThumbnailUrl: String? = null,
    @Embedded("Album_youtubePlaylist_") val youtubePlaylist: YoutubePlaylist? = null,
    @Embedded("Album_albumArt_") val albumArt: MediaStoreImage? = null,
) : Parcelable {
    val isOnYoutube: Boolean
        get() = youtubePlaylist != null

    val youtubeWebUrl: String?
        get() = youtubePlaylist?.let { "https://youtube.com/playlist?list=${it.id}" }

    @WorkerThread
    fun getDownloadDirDocumentFile(downloadRoot: DocumentFile, context: Context): DocumentFile? {
        var ret: DocumentFile? = downloadRoot
        getSubDirs(context).forEach { ret = ret?.createDirectoryIfNotExists(it) }
        return ret
    }

    suspend fun getFullImage(context: Context): ImageBitmap? =
        getFullImageBitmap(context)?.scaleToMaxSize(IMAGE_MAX_DP_FULL.dp, context)?.asImageBitmap()

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

    suspend fun getThumbnail(context: Context): ImageBitmap? =
        getThumbnailBitmap(context)?.scaleToMaxSize(IMAGE_MAX_DP_THUMBNAIL.dp, context)?.asImageBitmap()

    private suspend fun getFullImageBitmap(context: Context): Bitmap? =
        albumArt?.getFullImageBitmap(context)
            ?: lastFmFullImageUrl?.getBitmapByUrl()
            ?: youtubePlaylist?.fullImage?.url?.getBitmapByUrl()

    private fun getSubDirs(context: Context): List<String> =
        listOf(artist?.sanitizeFilename() ?: context.getString(R.string.unknown_artist), title.sanitizeFilename())

    private suspend fun getThumbnailBitmap(context: Context): Bitmap? =
        albumArt?.getThumbnailBitmap(context)
            ?: lastFmThumbnailUrl?.getBitmapByUrl()
            ?: youtubePlaylist?.thumbnail?.url?.getBitmapByUrl()

    override fun toString(): String = artist?.let { "$it - $title" } ?: title
}
