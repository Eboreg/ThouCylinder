package us.huseli.thoucylinder.dataclasses

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.net.toUri
import androidx.room.Embedded
import kotlinx.parcelize.Parcelize
import us.huseli.thoucylinder.dataclasses.entities.Track
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Parcelize
data class YoutubeVideo(
    val id: String,
    val title: String,
    val url: String? = null,
    val durationMs: Long? = null,
    @Embedded("metadata_") val metadata: YoutubeMetadata? = null,
    @Embedded("thumbnail_") val thumbnail: YoutubeThumbnail? = null,
) : Parcelable {
    val duration: Duration?
        get() = durationMs?.milliseconds ?: metadata?.durationMs?.milliseconds

    val expiresAt: Instant?
        get() = (uri ?: metadata?.uri)?.getQueryParameter("expire")?.toLong()?.let { Instant.ofEpochSecond(it) }

    val uri: Uri?
        get() = metadata?.uri ?: url?.toUri()

    suspend fun getImageBitmap(): ImageBitmap? = thumbnail?.getImageBitmap()

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
