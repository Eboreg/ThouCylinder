package us.huseli.thoucylinder.dataclasses.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import us.huseli.thoucylinder.enums.RadioType
import java.util.UUID

@Entity
data class Radio(
    @PrimaryKey @ColumnInfo("Radio_id") val id: String = UUID.randomUUID().toString(),
    @ColumnInfo("Radio_type") val type: RadioType,
    @ColumnInfo("Radio_artistId") val artistId: String? = null,
    @ColumnInfo("Radio_trackId") val trackId: String? = null,
    @ColumnInfo("Radio_albumId") val albumId: String? = null,
    @ColumnInfo("Radio_usedSpotifyTrackIds") val usedSpotifyTrackIds: String? = null,
    @ColumnInfo("Radio_isInitialized") val isInitialized: Boolean = false,
)
