package us.huseli.thoucylinder.dataclasses

import android.content.Context
import android.graphics.Bitmap
import android.os.Parcelable
import androidx.room.Embedded
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import us.huseli.retaintheme.toDuration
import us.huseli.thoucylinder.dataclasses.entities.Track
import java.util.UUID
import kotlin.time.Duration

@Parcelize
data class YoutubeVideo(
    val id: String,
    val title: String,
    val length: String? = null,
    val playlistItemId: String? = null,
    val playlistPosition: Int? = null,
    @Embedded("metadata_") val metadata: YoutubeMetadata? = null,
    @Embedded("thumbnail_") val thumbnail: YoutubeThumbnail? = null,
) : Parcelable {
    @IgnoredOnParcel
    val duration: Duration?
        get() = length?.toDuration()

    suspend fun getBitmap(): Bitmap? = thumbnail?.getBitmap()

    suspend fun saveMediaStoreImage(context: Context): MediaStoreImage? =
        thumbnail?.url?.let { MediaStoreImage.fromUrl(url = it, video = this, context = context) }

    fun toTrack(isInLibrary: Boolean, albumId: UUID? = null): Track = Track(
        title = title,
        isInLibrary = isInLibrary,
        youtubeVideo = this,
        albumPosition = playlistPosition,
        albumId = albumId,
        metadata = metadata?.toTrackMetadata(),
    )

    override fun toString(): String = if (duration != null) "$title ($duration)" else title
}
