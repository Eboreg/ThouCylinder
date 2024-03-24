package us.huseli.thoucylinder.dataclasses.views

import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.Relation
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.Track
import java.util.UUID

@DatabaseView(
    """
    SELECT TrackCombo.*, Playlist.*, PlaylistTrack_position, PlaylistTrack_id
    FROM TrackCombo 
        JOIN PlaylistTrack ON Track_trackId = PlaylistTrack_trackId 
        JOIN Playlist ON PlaylistTrack_playlistId = Playlist_playlistId 
    GROUP BY PlaylistTrack_id
    ORDER BY PlaylistTrack_position
    """
)
data class PlaylistTrackCombo(
    @Embedded override val track: Track,
    @Embedded override val album: Album?,
    @Embedded val playlist: Playlist,
    override val albumArtist: String?,
    @ColumnInfo("PlaylistTrack_position") val position: Int,
    @ColumnInfo("PlaylistTrack_id") val id: UUID,
    @Relation(parentColumn = "Track_trackId", entityColumn = "TrackArtist_trackId")
    override val artists: List<TrackArtistCredit>,
) : AbstractTrackCombo() {
    override fun equals(other: Any?) = other is PlaylistTrackCombo &&
        other.id == id &&
        other.position == position

    override fun hashCode() = 31 * (31 * super.hashCode() + position) + id.hashCode()
}
