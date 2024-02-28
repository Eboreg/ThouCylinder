package us.huseli.thoucylinder.dataclasses.pojos

import androidx.room.ColumnInfo
import java.util.UUID

data class TopLocalSpotifyArtistPojo(
    @ColumnInfo("Artist_id") val id: UUID,
    @ColumnInfo("Artist_name") val name: String,
    @ColumnInfo("Artist_spotifyId") val spotifyId: String,
    val trackCount: Int,
)
