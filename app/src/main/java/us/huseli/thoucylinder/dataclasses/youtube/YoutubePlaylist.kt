package us.huseli.thoucylinder.dataclasses.youtube

import android.os.Parcelable
import androidx.room.Embedded
import kotlinx.parcelize.Parcelize
import us.huseli.thoucylinder.dataclasses.MediaStoreImage

@Parcelize
data class YoutubePlaylist(
    val id: String,
    val title: String,
    val artist: String? = null,
    @Embedded("thumbnail_") val thumbnail: YoutubeImage? = null,
    @Embedded("fullImage_") val fullImage: YoutubeImage? = null,
    val videoCount: Int = 0,
) : Parcelable {
    suspend fun getMediaStoreImage(): MediaStoreImage? {
        val fullImageUrl = fullImage?.url ?: thumbnail?.url
        val thumbnailUrl = thumbnail?.url ?: fullImage?.url

        return if (fullImageUrl != null) MediaStoreImage.fromUrls(fullImageUrl, thumbnailUrl) else null
    }

    override fun toString() = "${artist?.let { "$it - $title" } ?: title} ($videoCount videos)"
}
