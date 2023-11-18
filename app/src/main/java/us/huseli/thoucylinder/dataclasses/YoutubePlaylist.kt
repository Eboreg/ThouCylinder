package us.huseli.thoucylinder.dataclasses

import android.graphics.Bitmap
import android.os.Parcelable
import androidx.room.Embedded
import kotlinx.parcelize.Parcelize
import us.huseli.thoucylinder.dataclasses.entities.Album

@Parcelize
data class YoutubePlaylist(
    val id: String,
    val title: String,
    val artist: String? = null,
    @Embedded("thumbnail_") val thumbnail: YoutubeThumbnail? = null,
    val videoCount: Int = 0,
) : Parcelable {
    suspend fun getBitmap(): Bitmap? = thumbnail?.getBitmap()

    fun toAlbum(isInLibrary: Boolean) = Album(
        title = title,
        isInLibrary = isInLibrary,
        isLocal = false,
        artist = artist,
        youtubePlaylist = this,
    )

    override fun toString() = "${artist?.let { "$it - $title" } ?: title} ($videoCount videos)"
}
