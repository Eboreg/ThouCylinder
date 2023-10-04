package us.huseli.thoucylinder.dataclasses

import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class ArtistPojo(
    val name: String,
    val albumCount: Int,
    val trackCount: Int,
    val firstAlbumArt: File?,
    val totalDurationMs: Long,
) {
    val totalDuration: Duration
        get() = totalDurationMs.milliseconds
}
