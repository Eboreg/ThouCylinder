package us.huseli.thoucylinder.dataclasses.pojos

import androidx.room.ColumnInfo

data class TopLocalSpotifyArtistPojo(
    @ColumnInfo("Artist_id") val id: String,
    @ColumnInfo("Artist_name") val name: String,
    @ColumnInfo("Artist_spotifyId") val spotifyId: String,
    val trackCount: Int,
)
