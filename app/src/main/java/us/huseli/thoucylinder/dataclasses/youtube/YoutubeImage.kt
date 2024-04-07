package us.huseli.thoucylinder.dataclasses.youtube

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlin.math.min

@Parcelize
data class YoutubeImage(
    val url: String,
    val width: Int,
    val height: Int,
) : Parcelable {
    val shortestSide: Int
        get() = min(width, height)
}
