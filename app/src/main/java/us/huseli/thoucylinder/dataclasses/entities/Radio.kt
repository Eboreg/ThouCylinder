package us.huseli.thoucylinder.dataclasses.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import us.huseli.thoucylinder.RadioType
import java.util.UUID

@Entity
data class Radio(
    @PrimaryKey @ColumnInfo("Radio_id") val id: UUID = UUID.randomUUID(),
    @ColumnInfo("Radio_type") val type: RadioType,
    @ColumnInfo("Radio_artistId") val artistId: UUID? = null,
    @ColumnInfo("Radio_trackId") val trackId: UUID? = null,
    @ColumnInfo("Radio_albumId") val albumId: UUID? = null,
    @ColumnInfo("Radio_usedSpotifyTrackIds") val usedSpotifyTrackIds: List<String> = emptyList(),
    @ColumnInfo("Radio_isInitialized") val isInitialized: Boolean = false,
)
