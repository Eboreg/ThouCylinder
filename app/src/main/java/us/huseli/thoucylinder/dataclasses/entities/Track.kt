package us.huseli.thoucylinder.dataclasses.entities

import android.content.Context
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.getAbsolutePath
import org.apache.commons.text.similarity.LevenshteinDistance
import us.huseli.retaintheme.sanitizeFilename
import us.huseli.retaintheme.scaleToMaxSize
import us.huseli.thoucylinder.Constants.IMAGE_MAX_DP_FULL
import us.huseli.thoucylinder.Constants.IMAGE_MAX_DP_THUMBNAIL
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.TrackMetadata
import us.huseli.thoucylinder.dataclasses.YoutubeVideo
import us.huseli.thoucylinder.getBitmapByUrl
import us.huseli.thoucylinder.nullIfEmpty
import java.util.UUID
import kotlin.time.Duration

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Album::class,
            parentColumns = ["Album_albumId"],
            childColumns = ["Track_albumId"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("Track_albumId"), Index("Track_title"), Index("Track_artist"), Index("Track_isInLibrary")],
)
data class Track(
    @ColumnInfo("Track_trackId") @PrimaryKey val trackId: UUID = UUID.randomUUID(),
    @ColumnInfo("Track_title") val title: String,
    @ColumnInfo("Track_isInLibrary") val isInLibrary: Boolean,
    @ColumnInfo("Track_artist") val artist: String? = null,
    @ColumnInfo("Track_albumId") val albumId: UUID? = null,
    @ColumnInfo("Track_albumPosition") val albumPosition: Int? = null,
    @ColumnInfo("Track_discNumber") val discNumber: Int? = null,
    @ColumnInfo("Track_year") val year: Int? = null,
    @ColumnInfo("Track_localUri") val localUri: Uri? = null,
    @Embedded("Track_metadata_") val metadata: TrackMetadata? = null,
    @Embedded("Track_youtubeVideo_") val youtubeVideo: YoutubeVideo? = null,
    @Embedded("Track_image_") val image: MediaStoreImage? = null,
) : Comparable<Track> {
    private val albumPositionNonNull: Int
        get() = albumPosition ?: 0

    val discNumberNonNull: Int
        get() = discNumber ?: 1

    val duration: Duration?
        get() = metadata?.duration ?: youtubeVideo?.duration

    val isDownloadable: Boolean
        get() = !isDownloaded && isOnYoutube

    val isDownloaded: Boolean
        get() = localUri != null

    val isOnYoutube: Boolean
        get() = youtubeVideo != null

    val playUri: Uri?
        get() = localUri ?: youtubeVideo?.metadata?.uri

    val youtubeWebUrl: String?
        get() = youtubeVideo?.let { "https://youtu.be/${it.id}" }

    fun generateBasename(albumArtist: String? = null): String {
        /** Filename will only include this.artist if it differs from albumArtist. */
        var name = ""
        if (albumPosition != null) name += "${String.format("%02d", albumPosition)} - "
        if (artist != null && artist != albumArtist) name += "$artist - "
        name += title

        return name.sanitizeFilename()
    }

    @WorkerThread
    fun getDocumentFile(context: Context): DocumentFile? =
        localUri?.let { DocumentFileCompat.fromUri(context, it) }

    @WorkerThread
    suspend fun getFullImage(context: Context): ImageBitmap? =
        (image?.getFullImageBitmap(context) ?: youtubeVideo?.fullImage?.url?.getBitmapByUrl())
            ?.scaleToMaxSize(IMAGE_MAX_DP_FULL.dp, context)?.asImageBitmap()

    fun getLevenshteinDistance(other: Track, albumArtist: String? = null): Int {
        val levenshtein = LevenshteinDistance()
        val distances = mutableListOf<Int>()

        distances.add(levenshtein.apply(title.lowercase(), other.title.lowercase()))
        if (albumArtist != null) {
            distances.add(levenshtein.apply("$albumArtist - $title".lowercase(), other.title.lowercase()))
            if (other.artist != null) distances.add(
                levenshtein.apply(
                    "$albumArtist - $title".lowercase(),
                    "${other.artist} ${other.title}".lowercase(),
                )
            )
        }
        if (artist != null) {
            distances.add(levenshtein.apply("$artist - $title".lowercase(), other.title.lowercase()))
            if (other.artist != null) distances.add(
                levenshtein.apply(
                    "$artist - $title".lowercase(),
                    "${other.artist} - ${other.title}".lowercase(),
                )
            )
        }
        if (other.artist != null)
            distances.add(levenshtein.apply(title.lowercase(), "${other.artist} - ${other.title}".lowercase()))

        return distances.min()
    }

    fun getLocalAbsolutePath(context: Context): String? =
        getDocumentFile(context)?.getAbsolutePath(context)?.nullIfEmpty()

    suspend fun getThumbnail(context: Context): ImageBitmap? =
        (image?.getThumbnailBitmap(context) ?: youtubeVideo?.thumbnail?.url?.getBitmapByUrl())
            ?.scaleToMaxSize(IMAGE_MAX_DP_THUMBNAIL.dp, context)?.asImageBitmap()

    fun toString(showAlbumPosition: Boolean, showArtist: Boolean): String {
        var string = ""
        if (albumPosition != null && showAlbumPosition) string += "$albumPosition. "
        if (artist != null && showArtist) string += "$artist - "
        string += title

        return string
    }

    override fun compareTo(other: Track): Int {
        if (discNumberNonNull != other.discNumberNonNull)
            return discNumberNonNull - other.discNumberNonNull
        if (albumPositionNonNull != other.albumPositionNonNull)
            return albumPositionNonNull - other.albumPositionNonNull
        return title.compareTo(other.title)
    }

    override fun toString(): String = toString(showAlbumPosition = true, showArtist = true)
}
