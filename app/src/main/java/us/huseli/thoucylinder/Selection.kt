package us.huseli.thoucylinder

import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.QueueTrackPojo
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo

data class Selection(
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val queueTracks: List<QueueTrackPojo> = emptyList(),
) {
    constructor(track: Track) : this(tracks = listOf(track))

    constructor(trackPojo: TrackPojo) : this(tracks = listOf(trackPojo.track))

    constructor(trackPojos: List<TrackPojo>) : this(tracks = trackPojos.map { it.track })

    constructor(album: Album) : this(albums = listOf(album))

    val trackCount: Int
        get() = tracks.size + queueTracks.size
}
