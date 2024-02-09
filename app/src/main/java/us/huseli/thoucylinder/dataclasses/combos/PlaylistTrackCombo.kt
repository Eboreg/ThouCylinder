package us.huseli.thoucylinder.dataclasses.combos

import androidx.room.ColumnInfo
import androidx.room.Embedded
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.PlaylistTrack
import us.huseli.thoucylinder.dataclasses.entities.SpotifyTrack
import us.huseli.thoucylinder.dataclasses.entities.Track

data class PlaylistTrackCombo(
    @Embedded override val track: Track,
    @Embedded override val album: Album?,
    @Embedded override val spotifyTrack: SpotifyTrack?,
    @Embedded val playlist: Playlist,
    @ColumnInfo(name = "PlaylistTrack_position") val position: Int,
) : AbstractTrackCombo() {
    fun toPlaylistTrack() =
        PlaylistTrack(playlistId = playlist.playlistId, trackId = track.trackId, position = position)

    override fun equals(other: Any?) = other is PlaylistTrackCombo &&
        other.track.trackId == track.trackId &&
        other.playlist.playlistId == playlist.playlistId &&
        other.position == position

    override fun hashCode(): Int = 31 * (31 * track.trackId.hashCode() + playlist.playlistId.hashCode()) + position
}

fun List<PlaylistTrackCombo>.toPlaylistTracks(): List<PlaylistTrack> = map { it.toPlaylistTrack() }
