package us.huseli.thoucylinder

import us.huseli.thoucylinder.dataclasses.Album
import us.huseli.thoucylinder.dataclasses.Track

data class Selection(
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
) {
    fun addAlbum(album: Album): Selection {
        if (!albums.map { it.albumId }.contains(album.albumId))
            return Selection(tracks = tracks, albums = albums + album)
        return this
    }

    fun isTrackSelected(track: Track) = tracks.map { it.trackId }.contains(track.trackId)

    fun removeAlbum(album: Album): Selection {
        if (albums.map { it.albumId }.contains(album.albumId))
            return Selection(tracks = tracks, albums = albums.filterNot { it.albumId == album.albumId })
        return this
    }

    fun toggleTrackSelected(track: Track): Selection =
        if (isTrackSelected(track)) removeTrack(track)
        else addTrack(track)

    private fun addTrack(track: Track): Selection {
        if (!tracks.map { it.trackId }.contains(track.trackId))
            return Selection(tracks = tracks + track, albums = albums)
        return this
    }

    private fun removeTrack(track: Track): Selection {
        if (tracks.map { it.trackId }.contains(track.trackId))
            return Selection(tracks = tracks.filterNot { it.trackId == track.trackId }, albums = albums)
        return this
    }
}
