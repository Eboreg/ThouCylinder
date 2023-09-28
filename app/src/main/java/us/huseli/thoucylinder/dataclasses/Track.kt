package us.huseli.thoucylinder.dataclasses

import android.net.Uri
import android.os.Environment
import androidx.core.net.toUri
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import us.huseli.retaintheme.sanitizeFilename
import java.io.File
import java.util.UUID

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Album::class,
            parentColumns = ["albumId"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("albumId")],
)
data class Track(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val title: String,
    val isInLibrary: Boolean,
    val artist: String? = null,
    val albumId: UUID? = null,
    val albumPosition: Int? = null,
    val year: Int? = null,
    @Embedded("local") val local: LocalTrackData? = null,
    @Embedded("metadata") val metadata: TrackMetadata? = null,
    @Embedded("youtubeVideo") val youtubeVideo: YoutubeVideo? = null,
    @Embedded("image") val image: Image? = null,
    @Ignore val tempTrackData: TempTrackData? = null,
) {
    constructor(
        id: UUID,
        title: String,
        isInLibrary: Boolean,
        artist: String?,
        albumId: UUID?,
        albumPosition: Int?,
        year: Int?,
        local: LocalTrackData?,
        metadata: TrackMetadata?,
        youtubeVideo: YoutubeVideo?,
        image: Image?,
    ) : this(id, title, isInLibrary, artist, albumId, albumPosition, year, local, metadata, youtubeVideo, image, null)

    val isDownloaded: Boolean
        get() = mediaStoreFile != null

    val isOnYoutube: Boolean
        get() = youtubeVideo != null

    /** Local filename without path or extension. */
    val localBasename: String
        get() {
            var name = ""
            if (albumPosition != null) name += "${String.format("%02d", albumPosition)} - "
            if (artist != null) name += "$artist - "
            name += title

            return name.sanitizeFilename()
        }

    /** Local filename without path but with extension. */
    private val localFilename: String?
        get() = metadata?.let { "$localBasename.${it.extension}".sanitizeFilename() }

    /** Relative local path without filename. */
    private val localSubdir: String?
        get() = local?.subdir?.sanitizeFilename()

    val localSubdirAndFilename: Pair<String, String>?
        get() {
            val subdir = localSubdir
            val filename = localFilename
            if (subdir != null && filename != null) return Pair(subdir, filename)
            return null
        }

    /** Relative local path with filename and extension. */
    private val localPath: String?
        get() = localSubdir?.let { "$it/$localFilename" }

    val mediaStoreFile: File?
        get() = localPath?.let { path ->
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), path).takeIf { it.isFile }
        }

    val mediaStoreUri: Uri?
        get() = mediaStoreFile?.toUri()

    val playUri: Uri?
        get() = mediaStoreUri ?: youtubeVideo?.metadata?.uri

    override fun toString(): String {
        var string = ""
        if (albumPosition != null) string += "$albumPosition. "
        if (artist != null) string += "$artist - "
        string += title

        return string
    }
}


data class TempTrackData(
    val localFile: File,
)


data class LocalTrackData(
    val subdir: String? = null,
)
