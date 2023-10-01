package us.huseli.thoucylinder.dataclasses

import androidx.room.Ignore

data class YoutubePlaylist(
    val id: String,
    val title: String,
    @Ignore val artist: String? = null,
    @Ignore val thumbnail: Image? = null,
    @Ignore val videoCount: Int = 0,
) {
    constructor(id: String, title: String) : this(id, title, null, null, 0)

    override fun toString(): String {
        return "${artist?.let { "$it - $title" } ?: title} ($videoCount videos)"
    }
}
