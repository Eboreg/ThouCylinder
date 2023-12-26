package us.huseli.thoucylinder.viewmodels

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import us.huseli.thoucylinder.listItemsBetween

abstract class AbstractSelectViewModel(
    private val selectionKey: String,
    private val repos: Repositories,
) : AbstractBaseViewModel(repos) {
    val selectedAlbums: StateFlow<List<Album>> = repos.album.flowSelectedAlbums(selectionKey)
    val selectedTrackPojos: StateFlow<List<TrackPojo>> = repos.track.flowSelectedTrackPojos(selectionKey)

    val latestSelectedTrackPojo = selectedTrackPojos.map { it.lastOrNull() }
    val latestSelectedAlbum = selectedAlbums.map { it.lastOrNull() }

    fun selectAlbums(albums: Iterable<Album>) = repos.album.selectAlbums(selectionKey, albums)

    fun selectAlbumsFromLastSelected(to: Album, allAlbums: List<Album>) {
        val albums = selectedAlbums.value.lastOrNull()
            ?.let { allAlbums.listItemsBetween(it, to).plus(to) }
            ?: listOf(to)

        repos.album.selectAlbums(selectionKey, albums)
    }

    fun selectTrackPojos(pojos: Iterable<TrackPojo>) = repos.track.selectTrackPojos(selectionKey, pojos)

    fun selectTrackPojosFromLastSelected(to: TrackPojo, allPojos: List<TrackPojo>) {
        val pojos = selectedTrackPojos.value.lastOrNull()
            ?.let { allPojos.listItemsBetween(it, to).plus(to) }
            ?: listOf(to)

        repos.track.selectTrackPojos(selectionKey, pojos)
    }

    fun toggleSelected(album: Album) = repos.album.toggleAlbumSelected(selectionKey, album)

    fun toggleSelected(trackPojo: TrackPojo) {
        repos.track.toggleTrackPojoSelected(selectionKey, trackPojo)
    }

    fun unselectAllAlbums() = repos.album.unselectAllAlbums(selectionKey)

    open fun unselectAllTrackPojos() = repos.track.unselectAllTrackPojos(selectionKey)
}
