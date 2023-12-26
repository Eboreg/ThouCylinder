package us.huseli.thoucylinder.dataclasses.pojos

import androidx.room.ColumnInfo
import androidx.room.Embedded
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.LastFmTrack
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.PlaylistTrack
import us.huseli.thoucylinder.dataclasses.entities.SpotifyTrack
import us.huseli.thoucylinder.dataclasses.entities.Track

data class PlaylistTrackPojo(
    @Embedded override val track: Track,
    @Embedded override val album: Album?,
    @Embedded override val spotifyTrack: SpotifyTrack?,
    @Embedded override val lastFmTrack: LastFmTrack?,
    @Embedded val playlist: Playlist,
    @ColumnInfo(name = "PlaylistTrack_position") val position: Int,
) : AbstractTrackPojo() {
    fun toPlaylistTrack() =
        PlaylistTrack(playlistId = playlist.playlistId, trackId = track.trackId, position = position)

    override fun equals(other: Any?) = other is PlaylistTrackPojo &&
        other.track.trackId == track.trackId &&
        other.playlist.playlistId == playlist.playlistId &&
        other.position == position

    override fun hashCode(): Int = 31 * (31 * track.trackId.hashCode() + playlist.playlistId.hashCode()) + position
}

fun List<PlaylistTrackPojo>.toPlaylistTracks(): List<PlaylistTrack> = map { it.toPlaylistTrack() }
