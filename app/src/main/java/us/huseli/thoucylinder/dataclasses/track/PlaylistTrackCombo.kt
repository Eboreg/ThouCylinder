package us.huseli.thoucylinder.dataclasses.track

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.Relation
import us.huseli.thoucylinder.dataclasses.album.Album
import us.huseli.thoucylinder.dataclasses.artist.AlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.TrackArtistCredit
import us.huseli.thoucylinder.dataclasses.playlist.Playlist

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
@Immutable
data class PlaylistTrackCombo(
    @Embedded override val track: Track,
    @Embedded override val album: Album?,
    @Embedded val playlist: Playlist,
    @ColumnInfo("PlaylistTrack_position") val position: Int,
    @ColumnInfo("PlaylistTrack_id") val id: String,
    @Relation(parentColumn = "Track_trackId", entityColumn = "TrackArtist_trackId", entity = TrackArtistCredit::class)
    override val trackArtists: List<TrackArtistCredit>,
    @Relation(parentColumn = "Track_albumId", entityColumn = "AlbumArtist_albumId", entity = AlbumArtistCredit::class)
    override val albumArtists: List<AlbumArtistCredit> = emptyList(),
) : ISavedTrackCombo {
    fun toPlainTrackCombo() = TrackCombo(
        track = track,
        album = album,
        trackArtists = trackArtists,
        albumArtists = albumArtists,
    )

    override fun toUiState(isSelected: Boolean): TrackUiState = super.toUiState(isSelected = isSelected).copy(id = id)

    override fun withTrack(track: Track) = copy(track = track)

    override fun equals(other: Any?) = other is PlaylistTrackCombo &&
        other.id == id &&
        other.position == position

    override fun hashCode() = 31 * (31 * super.hashCode() + position) + id.hashCode()
}

fun Iterable<PlaylistTrackCombo>.toPlainTrackCombos() = map { it.toPlainTrackCombo() }
