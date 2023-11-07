package us.huseli.thoucylinder.dataclasses.pojos

import android.net.Uri
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class ArtistPojo(
    val name: String,
    val albumCount: Int,
    val trackCount: Int,
    val firstAlbumArt: Uri?,
    val firstAlbumArtThumbnail: Uri?,
    val totalDurationMs: Long,
) {
    val totalDuration: Duration
        get() = totalDurationMs.milliseconds
}
