package us.huseli.thoucylinder.dataclasses.artist

import androidx.room.Embedded

data class ArtistWithCounts(
    @Embedded val artist: Artist,
    val trackCount: Int,
    val albumCount: Int,
)
