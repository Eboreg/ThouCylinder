package us.huseli.thoucylinder.data.entities

import kotlin.time.Duration

data class Track(
    val title: String,
    val youtubeId: String? = null,
    val length: Duration? = null,
) {
    override fun toString(): String = if (length != null) "$title ($length)" else title
}
