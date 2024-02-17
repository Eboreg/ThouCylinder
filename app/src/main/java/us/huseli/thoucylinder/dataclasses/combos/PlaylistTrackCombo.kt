package us.huseli.thoucylinder.dataclasses.combos

import androidx.room.ColumnInfo
import androidx.room.Embedded
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.PlaylistTrack
import us.huseli.thoucylinder.dataclasses.entities.Track
import java.util.UUID

data class PlaylistTrackCombo(
    @Embedded override val track: Track,
    @Embedded override val album: Album?,
    @Embedded val playlist: Playlist,
    @ColumnInfo("PlaylistTrack_position") val position: Int,
    @ColumnInfo("PlaylistTrack_id") val id: UUID,
) : AbstractTrackCombo() {
    override fun equals(other: Any?) = other is PlaylistTrackCombo &&
        other.id == id &&
        other.position == position

    override fun hashCode() = 31 * (31 * super.hashCode() + position) + id.hashCode()
}

fun List<PlaylistTrackCombo>.toPlaylistTracks(): List<PlaylistTrack> = map {
    PlaylistTrack(playlistId = it.playlist.playlistId, trackId = it.track.trackId, position = it.position, id = it.id)
}
