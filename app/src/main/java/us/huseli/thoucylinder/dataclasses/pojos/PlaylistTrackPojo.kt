package us.huseli.thoucylinder.dataclasses.pojos

import androidx.room.ColumnInfo
import androidx.room.Embedded
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.PlaylistTrack
import us.huseli.thoucylinder.dataclasses.entities.Track

data class PlaylistTrackPojo(
    @Embedded override val track: Track,
    @Embedded override val album: Album?,
    @Embedded val playlist: Playlist,
    @ColumnInfo(name = "PlaylistTrack_position") val position: Int,
) : AbstractTrackPojo() {
    fun toPlaylistTrack() =
        PlaylistTrack(playlistId = playlist.playlistId, trackId = track.trackId, position = position)

    override fun equals(other: Any?) = other is PlaylistTrackPojo &&
        other.track.trackId == track.trackId &&
        other.playlist.playlistId == playlist.playlistId &&
        other.position == position

    override fun hashCode(): Int {
        var result = track.trackId.hashCode()
        result = 31 * result + playlist.playlistId.hashCode()
        result = 31 * result + position
        return result
    }
}

fun List<PlaylistTrackPojo>.toPlaylistTracks(): List<PlaylistTrack> = map { it.toPlaylistTrack() }
