package us.huseli.thoucylinder.dataclasses

import android.net.Uri
import android.os.Environment
import androidx.core.net.toUri
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import us.huseli.retaintheme.sensibleFormat
import us.huseli.thoucylinder.database.Converters
import us.huseli.thoucylinder.sanitizeFilename
import java.io.File
import java.util.UUID

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Album::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("albumId")],
)
@TypeConverters(Converters::class)
data class Track(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val title: String,
    val artist: String? = null,
    val albumId: UUID? = null,
    val albumPosition: Int? = null,
    val localSubdir: String? = null,
    @Embedded("metadata") val metadata: TrackMetadata,
    @Embedded("youtubeVideo") val youtubeVideo: YoutubeVideo? = null,
    @Embedded("image") val image: Image? = null,
) {
    private val localFilename: String
        get() {
            var name = ""
            if (albumPosition != null) name += "${String.format("%02d", albumPosition)} - "
            if (artist != null) name += "$artist - "
            name += title

            return "$name.${metadata.extension}".sanitizeFilename()
        }

    private val localPath: String
        get() = localSubdir?.let { "$localSubdir/$localFilename" } ?: localFilename

    val localFile: File?
        get() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), localPath)
            .takeIf { it.isFile }

    val localUri: Uri?
        get() = localFile?.toUri()

    override fun toString(): String =
        (artist?.let { "$artist - " } ?: "") + "$title (${metadata.duration.sensibleFormat()})"
}
