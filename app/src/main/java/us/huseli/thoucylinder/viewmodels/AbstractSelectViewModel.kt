package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import us.huseli.thoucylinder.repositories.Repositories
import kotlin.math.max
import kotlin.math.min

abstract class AbstractSelectViewModel(
    private val selectionKey: String,
    private val repos: Repositories,
) : AbstractBaseViewModel(repos) {
    val selectedAlbums = repos.room.getSelectedAlbumFlow(selectionKey)
    val selectedTrackPojos = repos.room.getSelectedTrackPojoFlow(selectionKey)

    fun selectAlbums(albums: List<Album>) = repos.room.selectAlbums(selectionKey, albums)

    fun selectAlbumsFromLastSelected(albums: List<Album>, to: Album) {
        val lastSelected = selectedAlbums.value.lastOrNull()

        if (lastSelected != null) {
            val thisIdx = albums.indexOf(to)
            val lastSelectedIdx = albums.indexOf(lastSelected)

            repos.room.selectAlbums(
                selectionKey = selectionKey,
                albums = albums.subList(min(thisIdx, lastSelectedIdx), max(thisIdx, lastSelectedIdx) + 1),
            )
        } else {
            repos.room.selectAlbums(selectionKey, listOf(to))
        }
    }

    fun selectTrackPojosFromLastSelected(to: TrackPojo) = viewModelScope.launch {
        val lastSelected = selectedTrackPojos.value.lastOrNull()

        if (lastSelected != null) {
            val tracks = repos.room.listTrackPojosBetween(lastSelected, to)
            repos.room.selectTrackPojos(selectionKey, tracks)
        } else {
            repos.room.selectTrackPojos(selectionKey, listOf(to))
        }
    }

    fun toggleSelected(album: Album) = repos.room.toggleAlbumSelected(selectionKey, album)

    fun toggleSelected(pojo: TrackPojo) = repos.room.toggleTrackPojoSelected(selectionKey, pojo)

    fun unselectAllAlbums() = repos.room.unselectAllAlbums(selectionKey)

    open fun unselectAllTrackPojos() = repos.room.unselectAllTrackPojos(selectionKey)
}
