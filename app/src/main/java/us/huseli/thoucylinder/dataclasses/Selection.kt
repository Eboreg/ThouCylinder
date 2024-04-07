package us.huseli.thoucylinder.dataclasses

import kotlinx.collections.immutable.ImmutableCollection
import kotlinx.collections.immutable.persistentListOf
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track

data class Selection(
    val tracks: ImmutableCollection<Track> = persistentListOf(),
    val albums: ImmutableCollection<Album> = persistentListOf(),
    val albumsWithTracks: ImmutableCollection<AlbumWithTracksCombo> = persistentListOf(),
) {
    constructor(track: Track) : this(tracks = persistentListOf(track))

    constructor(album: Album) : this(albums = persistentListOf(album))

    constructor(albumWithTracks: AlbumWithTracksCombo) : this(albumsWithTracks = persistentListOf(albumWithTracks))
}
