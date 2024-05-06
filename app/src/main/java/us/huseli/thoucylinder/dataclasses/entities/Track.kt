package us.huseli.thoucylinder.dataclasses.entities

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.getAbsolutePath
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import us.huseli.retaintheme.extensions.nullIfEmpty
import us.huseli.retaintheme.extensions.sanitizeFilename
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.TrackMetadata
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.youtube.YoutubeVideo
import us.huseli.thoucylinder.getParentDirectory
import us.huseli.thoucylinder.matchFiles
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Album::class,
            parentColumns = ["Album_albumId"],
            childColumns = ["Track_albumId"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("Track_albumId"), Index("Track_title"), Index("Track_isInLibrary")],
)
@Immutable
data class Track(
    @ColumnInfo("Track_trackId") @PrimaryKey val trackId: String = UUID.randomUUID().toString(),
    @ColumnInfo("Track_title") val title: String,
    @ColumnInfo("Track_isInLibrary") val isInLibrary: Boolean = true,
    @ColumnInfo("Track_albumId") val albumId: String? = null,
    @ColumnInfo("Track_albumPosition") val albumPosition: Int? = null,
    @ColumnInfo("Track_discNumber") val discNumber: Int? = null,
    @ColumnInfo("Track_year") val year: Int? = null,
    @ColumnInfo("Track_localUri") val localUri: String? = null,
    @ColumnInfo("Track_musicBrainzId") val musicBrainzId: String? = null,
    @ColumnInfo("Track_spotifyId") val spotifyId: String? = null,
    @ColumnInfo("Track_durationMs") val durationMs: Long? = null,
    @ColumnInfo("Track_amplitudes") val amplitudes: String? = null,
    @Embedded("Track_metadata_") val metadata: TrackMetadata? = null,
    @Embedded("Track_youtubeVideo_") val youtubeVideo: YoutubeVideo? = null,
    @Embedded("Track_image_") val image: MediaStoreImage? = null,
) : Comparable<Track> {
    val albumPositionNonNull: Int
        get() = albumPosition ?: 0

    val amplitudeList: ImmutableList<Int>?
        get() = amplitudes?.let { a -> a.split(',').map { it.toInt() } }?.toImmutableList()

    val discNumberNonNull: Int
        get() = discNumber ?: 1

    val duration: Duration?
        get() = durationMs?.milliseconds ?: metadata?.duration ?: youtubeVideo?.duration

    val isDownloadable: Boolean
        get() = !isDownloaded && isOnYoutube

    val isDownloaded: Boolean
        get() = localUri != null

    val isOnSpotify: Boolean
        get() = spotifyId != null

    val isOnYoutube: Boolean
        get() = youtubeVideo != null

    val isPlayable: Boolean
        get() = localUri != null || youtubeVideo != null

    val lofiUri: String?
        get() = localUri ?: youtubeVideo?.metadata?.lofiUrl ?: youtubeVideo?.metadata?.url

    val playUri: String?
        get() = localUri ?: youtubeVideo?.metadata?.url

    val spotifyWebUrl: String?
        get() = spotifyId?.let { "https://open.spotify.com/track/${it}" }

    val youtubeWebUrl: String?
        get() = youtubeVideo?.let { "https://youtu.be/${it.id}" }

    fun generateBasename(includeArtist: Boolean = false, artist: String? = null): String {
        var name = ""
        if (albumPosition != null) name += "${String.format("%02d", albumPosition)} - "
        if (artist != null && includeArtist) name += "$artist - "
        name += title

        return name.sanitizeFilename()
    }

    @WorkerThread
    fun getDocumentFile(context: Context): DocumentFile? =
        localUri?.let { DocumentFileCompat.fromUri(context, it.toUri()) }

    fun getFileSize(context: Context) = getDocumentFile(context)?.length()

    fun getLocalAbsolutePath(context: Context): String? =
        getDocumentFile(context)?.getAbsolutePath(context)?.nullIfEmpty()

    fun getPositionString(albumDiscCount: Int): String =
        if (albumDiscCount > 1 && discNumber != null && albumPosition != null) "$discNumber.$albumPosition"
        else albumPosition?.toString() ?: ""

    fun toString(
        showAlbumPosition: Boolean = true,
        showYear: Boolean = false,
        albumCombo: AlbumWithTracksCombo? = null,
    ): String {
        var string = ""
        if (showAlbumPosition) {
            if (albumCombo != null) string += getPositionString(albumCombo.discCount) + ". "
            else if (albumPosition != null) string += "$albumPosition. "
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

    override fun toString(): String = toString(showAlbumPosition = true, showYear = false)
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
