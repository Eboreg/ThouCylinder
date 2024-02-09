package us.huseli.thoucylinder.dataclasses

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import us.huseli.retaintheme.extensions.pow
import kotlin.math.min

@Parcelize
data class YoutubeImage(
    val url: String,
    val width: Int,
    val height: Int,
) : Parcelable {
    val size: Int
        get() = min(width, height).pow(2)
}
