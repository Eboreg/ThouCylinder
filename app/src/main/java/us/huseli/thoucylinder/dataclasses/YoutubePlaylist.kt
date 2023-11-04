package us.huseli.thoucylinder.dataclasses

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Embedded
import us.huseli.thoucylinder.dataclasses.entities.Album

data class YoutubePlaylist(
    val id: String,
    val title: String,
    val artist: String? = null,
    @Embedded("thumbnail_") val thumbnail: YoutubeThumbnail? = null,
    val videoCount: Int = 0,
) {
    suspend fun getBitmap(): Bitmap? = thumbnail?.getBitmap()

    suspend fun saveMediaStoreImage(context: Context): MediaStoreImage? =
        thumbnail?.url?.let { MediaStoreImage.fromUrl(url = it, playlist = this, context = context) }

    fun toAlbum(isInLibrary: Boolean): Album = Album(
        title = title,
        isInLibrary = isInLibrary,
        isLocal = false,
        artist = artist,
        youtubePlaylist = this,
    )

    override fun toString() = "${artist?.let { "$it - $title" } ?: title} ($videoCount videos)"
}
