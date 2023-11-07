package us.huseli.thoucylinder.dataclasses

import android.content.Context
import android.graphics.Bitmap
import android.os.Parcelable
import androidx.room.Embedded
import kotlinx.parcelize.Parcelize
import us.huseli.thoucylinder.dataclasses.entities.Track
import java.util.UUID

@Parcelize
data class YoutubeVideo(
    val id: String,
    val title: String,
    @Embedded("metadata_") val metadata: YoutubeMetadata? = null,
    @Embedded("thumbnail_") val thumbnail: YoutubeThumbnail? = null,
) : Parcelable {
    suspend fun getBitmap(): Bitmap? = thumbnail?.getBitmap()

    suspend fun saveMediaStoreImage(context: Context): MediaStoreImage? =
        thumbnail?.url?.let { MediaStoreImage.fromUrl(url = it, video = this, context = context) }

    fun toTrack(isInLibrary: Boolean, albumPosition: Int? = null, albumId: UUID? = null): Track = Track(
        title = title,
        isInLibrary = isInLibrary,
        youtubeVideo = this,
        albumPosition = albumPosition,
        metadata = metadata?.toTrackMetadata(),
        albumId = albumId,
    )
}
