package us.huseli.thoucylinder

import us.huseli.thoucylinder.dataclasses.abstr.AbstractQueueTrack
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track

@Suppress("unused")
data class Selection(
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val queueTracks: List<AbstractQueueTrack> = emptyList(),
) {
    constructor(track: Track) : this(tracks = listOf(track))

    constructor(queueTrack: AbstractQueueTrack) : this(
        tracks = emptyList(),
        albums = emptyList(),
        queueTracks = listOf(queueTrack)
    )

    val trackCount: Int
        get() = tracks.size + queueTracks.size

    fun isSelected(queueTrack: AbstractQueueTrack) =
        queueTracks.map { it.queueTrackId }.contains(queueTrack.queueTrackId)

    fun isSelected(track: Track) = tracks.map { it.trackId }.contains(track.trackId)

    fun toggleSelected(queueTrack: AbstractQueueTrack): Selection =
        if (isSelected(queueTrack)) remove(queueTrack)
        else add(queueTrack)

    fun toggleSelected(track: Track): Selection =
        if (isSelected(track)) remove(track)
        else add(track)

    private fun add(album: Album): Selection =
        Selection(tracks = tracks, albums = albums + album, queueTracks = queueTracks)

    private fun add(queueTrack: AbstractQueueTrack): Selection =
        Selection(tracks = tracks, albums = albums, queueTracks = queueTracks + queueTrack)

    private fun add(track: Track): Selection =
        Selection(tracks = tracks + track, albums = albums, queueTracks = queueTracks)

    private fun remove(album: Album): Selection = Selection(
        tracks = tracks,
        albums = albums.filterNot { it.albumId == album.albumId },
        queueTracks = queueTracks,
    )

    private fun remove(queueTrack: AbstractQueueTrack): Selection = Selection(
        tracks = tracks,
        albums = albums,
        queueTracks = queueTracks.filterNot { it.queueTrackId == queueTrack.queueTrackId },
    )

    private fun remove(track: Track): Selection = Selection(
        tracks = tracks.filterNot { it.trackId == track.trackId },
        albums = albums,
        queueTracks = queueTracks,
    )
}
