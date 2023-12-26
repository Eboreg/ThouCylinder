package us.huseli.thoucylinder.dataclasses

import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.pojos.QueueTrackPojo

data class Selection(
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val queueTracks: List<QueueTrackPojo> = emptyList(),
    val albumsWithTracks: List<AlbumWithTracksPojo> = emptyList(),
) {
    constructor(track: Track) : this(tracks = listOf(track))

    constructor(album: Album) : this(albums = listOf(album))

    constructor(albumWithTracks: AlbumWithTracksPojo) : this(albumsWithTracks = listOf(albumWithTracks))

    val trackCount: Int
        get() = tracks.size + queueTracks.size
}
