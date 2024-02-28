package us.huseli.thoucylinder.dataclasses.combos

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Relation
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit
import java.util.UUID

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
