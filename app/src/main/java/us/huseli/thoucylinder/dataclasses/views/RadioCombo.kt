package us.huseli.thoucylinder.dataclasses.views

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.enums.RadioType
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.entities.RadioTrack
import us.huseli.thoucylinder.dataclasses.entities.Track
import java.util.UUID

@DatabaseView(
    """
    SELECT Radio_id, Radio_type, Radio_usedSpotifyTrackIds, Radio_isInitialized,
        COALESCE(Artist_name, Album_title, Track_title) AS Radio_title, Artist.*, Album.*, Track.*
    FROM Radio
        LEFT JOIN Artist ON Radio_artistId = Artist_id
        LEFT JOIN Album ON Radio_albumId = Album_albumId
        LEFT JOIN Track ON Radio_trackId = Track_trackId
    """
)
@Immutable
data class RadioCombo(
    @ColumnInfo("Radio_id") val id: String = UUID.randomUUID().toString(),
    @ColumnInfo("Radio_type") val type: RadioType,
    @ColumnInfo("Radio_title") val title: String?,
    @ColumnInfo("Radio_usedSpotifyTrackIds") val usedSpotifyTrackIds: String? = null,
    @ColumnInfo("Radio_isInitialized") val isInitialized: Boolean = false,
    @Embedded val artist: Artist?,
    @Embedded val album: Album?,
    @Embedded val track: Track?,
    @Relation(
        entity = Track::class,
        parentColumn = "Radio_id",
        entityColumn = "Track_trackId",
        associateBy = Junction(
            value = RadioTrack::class,
            parentColumn = "RadioTrack_radioId",
            entityColumn = "RadioTrack_trackId",
        ),
    )
    val usedLocalTracks: List<Track>,
) {
    fun getFullTitle(context: Context) =
        if (type == RadioType.LIBRARY) context.getString(R.string.library_radio)
        else context.getString(R.string.x_x_radio, title, context.getString(type.stringRes).lowercase())
}
