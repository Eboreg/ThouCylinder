package us.huseli.thoucylinder.dataclasses.spotify

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo

@Immutable
data class TopLocalSpotifyArtistPojo(
    @ColumnInfo("Artist_id") val id: String,
    @ColumnInfo("Artist_name") val name: String,
    @ColumnInfo("Artist_spotifyId") val spotifyId: String,
    val trackCount: Int,
)
