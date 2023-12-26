package us.huseli.thoucylinder.dataclasses

import android.os.Parcelable
import androidx.room.Embedded
import kotlinx.parcelize.Parcelize

@Parcelize
data class YoutubePlaylist(
    val id: String,
    val title: String,
    val artist: String? = null,
    @Embedded("thumbnail_") val thumbnail: YoutubeImage? = null,
    @Embedded("fullImage_") val fullImage: YoutubeImage? = null,
    val videoCount: Int = 0,
) : Parcelable {
    override fun toString() = "${artist?.let { "$it - $title" } ?: title} ($videoCount videos)"
}
