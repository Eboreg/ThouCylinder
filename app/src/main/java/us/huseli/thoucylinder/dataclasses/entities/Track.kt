package us.huseli.thoucylinder.dataclasses.entities

import android.content.Context
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.compose.ui.graphics.ImageBitmap
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
import us.huseli.retaintheme.extensions.nullIfEmpty
import us.huseli.retaintheme.extensions.sanitizeFilename
import us.huseli.retaintheme.extensions.stripCommonFixes
import us.huseli.thoucylinder.asThumbnailImageBitmap
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.TrackMetadata
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.youtube.YoutubeVideo
import us.huseli.thoucylinder.getBitmapByUrl
import us.huseli.thoucylinder.getParentDirectory
import us.huseli.thoucylinder.matchFiles
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
    @ColumnInfo("Track_isInLibrary") val isInLibrary: Boolean = true,
    @ColumnInfo("Track_artist") val artist: String? = null,
    @ColumnInfo("Track_albumId") val albumId: UUID? = null,
    @ColumnInfo("Track_albumPosition") val albumPosition: Int? = null,
    @ColumnInfo("Track_discNumber") val discNumber: Int? = null,
    @ColumnInfo("Track_year") val year: Int? = null,
    @ColumnInfo("Track_localUri") val localUri: Uri? = null,
    @ColumnInfo("Track_musicBrainzId") val musicBrainzId: String? = null,
    @ColumnInfo("Track_spotifyId") val spotifyId: String? = null,
    @Embedded("Track_metadata_") val metadata: TrackMetadata? = null,
    @Embedded("Track_youtubeVideo_") val youtubeVideo: YoutubeVideo? = null,
    @Embedded("Track_image_") val image: MediaStoreImage? = null,
) : Comparable<Track> {
    private val albumPositionNonNull: Int
        get() = albumPosition ?: 0

    private val discNumberNonNull: Int
        get() = discNumber ?: 1

    val duration: Duration?
        get() = metadata?.duration ?: youtubeVideo?.duration

    val isDownloadable: Boolean
        get() = !isDownloaded && isOnYoutube

    val isDownloaded: Boolean
        get() = localUri != null

    val isOnSpotify: Boolean
        get() = spotifyId != null

    val isOnYoutube: Boolean
        get() = youtubeVideo != null

    val playUri: Uri?
        get() = localUri ?: youtubeVideo?.metadata?.uri

    val spotifyWebUrl: String?
        get() = spotifyId?.let { "https://open.spotify.com/track/${it}" }

    val youtubeWebUrl: String?
        get() = youtubeVideo?.let { "https://youtu.be/${it.id}" }

    fun generateBasename(includeArtist: Boolean = false): String {
        var name = ""
        if (albumPosition != null) name += "${String.format("%02d", albumPosition)} - "
        if (artist != null && includeArtist) name += "$artist - "
        name += title

        return name.sanitizeFilename()
    }

    @WorkerThread
    fun getDocumentFile(context: Context): DocumentFile? =
        localUri?.let { DocumentFileCompat.fromUri(context, it) }

    @Suppress("unused")
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

    fun getPositionString(albumDiscCount: Int): String =
        if (albumDiscCount > 1 && discNumber != null && albumPosition != null) "$discNumber.$albumPosition"
        else albumPosition?.toString() ?: ""

    suspend fun getThumbnail(context: Context): ImageBitmap? = image?.getThumbnailImageBitmap(context)
        ?: youtubeVideo?.thumbnail?.url?.getBitmapByUrl()?.asThumbnailImageBitmap(context)

    fun toString(
        showAlbumPosition: Boolean = true,
        showArtist: Boolean = true,
        showYear: Boolean = false,
        showArtistIfSameAsAlbumArtist: Boolean = false,
        albumCombo: AlbumWithTracksCombo? = null,
    ): String {
        var string = ""
        if (showAlbumPosition) {
            if (albumCombo != null) string += getPositionString(albumCombo.discCount) + ". "
            else if (albumPosition != null) string += "$albumPosition. "
        }
        if (showArtist) {
            val trackArtist = artist
            val albumArtist = albumCombo?.album?.artist

            if (trackArtist != null && (showArtistIfSameAsAlbumArtist || trackArtist != albumArtist))
                string += "$trackArtist - "
            else if (albumArtist != null && showArtistIfSameAsAlbumArtist)
                string += "$albumArtist - "
        }
        string += title
        if (year != null && showYear) string += " ($year)"

        return string
    }

    override fun compareTo(other: Track): Int {
        if (discNumberNonNull != other.discNumberNonNull)
            return discNumberNonNull - other.discNumberNonNull
        if (albumPositionNonNull != other.albumPositionNonNull)
            return albumPositionNonNull - other.albumPositionNonNull
        return title.compareTo(other.title)
    }

    override fun toString(): String = toString(showAlbumPosition = true, showArtist = true, showYear = false)
}

@WorkerThread
fun Iterable<Track>.listParentDirectories(context: Context): List<DocumentFile> =
    mapNotNull { it.getDocumentFile(context)?.getParentDirectory(context) }.distinctBy { it.uri.path }

@WorkerThread
fun Iterable<Track>.listCoverImages(context: Context, includeThumbnails: Boolean = false): List<DocumentFile> {
    val filenameRegex =
        if (includeThumbnails) Regex("^cover(-thumbnail)?\\..*", RegexOption.IGNORE_CASE)
        else Regex("^cover\\..*", RegexOption.IGNORE_CASE)

    return listParentDirectories(context)
        .map { it.matchFiles(filenameRegex, Regex("^image/.*")) }
        .flatten()
        .distinctBy { it.uri.path }
}

fun Iterable<Track>.stripTitleCommons(): List<Track> = zip(map { it.title }.stripCommonFixes())
    .map { (track, title) -> track.copy(title = title.replace(Regex(" \\([^)]*$"), "")) }
