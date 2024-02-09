package us.huseli.thoucylinder.dataclasses.entities

import android.content.Context
import android.os.Parcelable
import androidx.annotation.WorkerThread
import androidx.compose.ui.graphics.ImageBitmap
import androidx.documentfile.provider.DocumentFile
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.anggrayudi.storage.file.CreateMode
import com.anggrayudi.storage.file.makeFolder
import kotlinx.parcelize.Parcelize
import org.apache.commons.text.similarity.LevenshteinDistance
import us.huseli.retaintheme.extensions.sanitizeFilename
import us.huseli.thoucylinder.R
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
    @ColumnInfo("Album_isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo("Album_isHidden") val isHidden: Boolean = false,
    @ColumnInfo("Album_musicBrainzReleaseId") val musicBrainzReleaseId: String? = null,
    @ColumnInfo("Album_musicBrainzReleaseGroupId") val musicBrainzReleaseGroupId: String? = null,
    @Embedded("Album_youtubePlaylist_") val youtubePlaylist: YoutubePlaylist? = null,
    @Embedded("Album_albumArt_") val albumArt: MediaStoreImage? = null,
) : Parcelable {
    val isOnYoutube: Boolean
        get() = youtubePlaylist != null

    val youtubeWebUrl: String?
        get() = youtubePlaylist?.let { "https://youtube.com/playlist?list=${it.id}" }

    @WorkerThread
    fun createDirectory(downloadRoot: DocumentFile, context: Context): DocumentFile? =
        downloadRoot.makeFolder(context, getSubDirs(context).joinToString("/"), CreateMode.REUSE)

    @WorkerThread
    fun getDirectory(downloadRoot: DocumentFile, context: Context): DocumentFile? {
        var ret: DocumentFile? = downloadRoot
        getSubDirs(context).forEach { dirname ->
            ret = ret?.findFile(dirname)?.takeIf { it.isDirectory }
        }
        return ret
    }

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

    suspend fun getThumbnail(context: Context): ImageBitmap? = albumArt?.getThumbnailImageBitmap(context)

    @WorkerThread
    suspend fun saveInternalAlbumArtFiles(imageUrl: String, context: Context): MediaStoreImage? =
        MediaStoreImage.fromUrls(imageUrl).saveInternal(this, context)

    private fun getSubDirs(context: Context): List<String> =
        listOf(artist?.sanitizeFilename() ?: context.getString(R.string.unknown_artist), title.sanitizeFilename())

    override fun toString(): String = artist?.let { "$it - $title" } ?: title
}
