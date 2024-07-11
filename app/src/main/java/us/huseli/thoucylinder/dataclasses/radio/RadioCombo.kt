package us.huseli.thoucylinder.dataclasses.radio

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Embedded
import us.huseli.thoucylinder.dataclasses.album.Album
import us.huseli.thoucylinder.dataclasses.artist.Artist
import us.huseli.thoucylinder.dataclasses.track.Track
import us.huseli.thoucylinder.enums.RadioType
import java.util.UUID

@DatabaseView(
    """
    SELECT Radio_id, Radio_type, Radio_usedSpotifyTrackIds, Radio_isInitialized,
        COALESCE(Artist_name, Album_title, Track_title) AS Radio_title, Artist.*, Album.*, Track.*,
        GROUP_CONCAT(DISTINCT QUOTE(RadioTrack_trackId)) AS Radio_usedLocalTrackIds
    FROM Radio
        LEFT JOIN Artist ON Radio_artistId = Artist_id
        LEFT JOIN Album ON Radio_albumId = Album_albumId
        LEFT JOIN Track ON Radio_trackId = Track_trackId
        LEFT JOIN RadioTrack ON Radio_id = RadioTrack_radioId AND RadioTrack_trackId IS NOT NULL
    GROUP BY Radio_id
    """
)
@Immutable
data class RadioCombo(
    @ColumnInfo("Radio_id") val id: String = UUID.randomUUID().toString(),
    @ColumnInfo("Radio_type") val type: RadioType,
    @ColumnInfo("Radio_title") val title: String?,
    @ColumnInfo("Radio_usedSpotifyTrackIds") val usedSpotifyTrackIds: List<String>,
    @ColumnInfo("Radio_isInitialized") val isInitialized: Boolean,
    @Embedded val artist: Artist?,
    @Embedded val album: Album?,
    @Embedded val track: Track?,
    @ColumnInfo("Radio_usedLocalTrackIds") val usedLocalTrackIds: List<String?>,
) {
    override fun equals(other: Any?) = other is RadioCombo && other.id == id

    override fun hashCode(): Int = id.hashCode()
}
