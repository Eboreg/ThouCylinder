package us.huseli.thoucylinder.dataclasses

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SpotifyAlbumArt(
    val url: String,
    val height: Int?,
    val width: Int?,
) : Parcelable {
    val size: Int
        get() = (height ?: 0) * (width ?: 0)
}
