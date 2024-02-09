package us.huseli.thoucylinder.dataclasses

import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.combos.QueueTrackCombo

data class Selection(
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val queueTracks: List<QueueTrackCombo> = emptyList(),
    val albumsWithTracks: List<AlbumWithTracksCombo> = emptyList(),
) {
    constructor(track: Track) : this(tracks = listOf(track))

    constructor(album: Album) : this(albums = listOf(album))

    constructor(albumWithTracks: AlbumWithTracksCombo) : this(albumsWithTracks = listOf(albumWithTracks))

    val trackCount: Int
        get() = tracks.size + queueTracks.size
}
