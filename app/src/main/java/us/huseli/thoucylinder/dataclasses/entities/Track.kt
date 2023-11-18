package us.huseli.thoucylinder.dataclasses.entities

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.apache.commons.text.similarity.LevenshteinDistance
import us.huseli.retaintheme.sanitizeFilename
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.TrackMetadata
import us.huseli.thoucylinder.dataclasses.YoutubeVideo
import us.huseli.thoucylinder.dataclasses.getMediaStoreFileNullable
import java.io.File
import java.util.UUID
import kotlin.time.Duration

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Album::class,
            parentColumns = ["Album_albumId"],
            childColumns = ["Track_albumId"],
            onDelete = ForeignKey.RESTRICT,
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
    @Embedded("Track_metadata_") val metadata: TrackMetadata? = null,
    @Embedded("Track_youtubeVideo_") val youtubeVideo: YoutubeVideo? = null,
    @Embedded("Track_image_") val image: MediaStoreImage? = null,
    @Embedded("Track_mediaStoreData_") val mediaStoreData: MediaStoreData? = null,
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
        get() = mediaStoreData != null

    val isOnYoutube: Boolean
        get() = youtubeVideo != null

    val playUri: Uri?
        get() = mediaStoreData?.uri ?: youtubeVideo?.uri

    fun generateBasename(): String {
        var name = ""
        if (albumPosition != null) name += "${String.format("%02d", albumPosition)} - "
        if (artist != null) name += "$artist - "
        name += title

        return name.sanitizeFilename()
    }

    suspend fun getFullImage(context: Context): Bitmap? = image?.getBitmap(context) ?: youtubeVideo?.getBitmap()

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

    suspend fun getThumbnail(context: Context): Bitmap? =
        image?.getThumbnailBitmap(context) ?: youtubeVideo?.getBitmap()

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


data class MediaStoreData(
    val uri: Uri,
    val relativePath: String,
) {
    fun getFile(context: Context): File? = context.getMediaStoreFileNullable(uri)
}
