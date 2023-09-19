package us.huseli.thoucylinder.data.entities

import android.os.Environment
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import us.huseli.retaintheme.sensibleFormat
import us.huseli.thoucylinder.data.Converters
import us.huseli.thoucylinder.toDuration
import java.io.File
import java.util.UUID
import kotlin.time.Duration

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
    val localPath: String,
    val length: String? = null,
    val artist: String? = null,
    val albumId: UUID? = null,
    val albumPosition: Int? = null,
    @Embedded("youtubeVideo") val youtubeVideo: YoutubeVideo? = null,
    @Embedded("image") val image: Image? = null,
) {
    @Ignore
    val duration: Duration? = length?.toDuration()

    val absolutePath: File
        get() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), localPath)

    override fun toString(): String =
        (artist?.let { "$artist - " } ?: "") + title + (duration?.let { " (${duration.sensibleFormat()})" } ?: "")
}
