package us.huseli.thoucylinder.dataclasses

import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track

data class Selection(
    val tracks: Collection<Track> = emptyList(),
    val albums: Collection<Album> = emptyList(),
    val albumsWithTracks: Collection<AlbumWithTracksCombo> = emptyList(),
) {
    constructor(track: Track) : this(tracks = listOf(track))

    constructor(album: Album) : this(albums = listOf(album))

    constructor(albumWithTracks: AlbumWithTracksCombo) : this(albumsWithTracks = listOf(albumWithTracks))
}
