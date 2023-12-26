package us.huseli.thoucylinder.dataclasses

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class YoutubeImage(
    val url: String,
    val width: Int,
    val height: Int,
) : Parcelable {
    val size: Int
        get() = width * height
}
