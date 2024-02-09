package us.huseli.thoucylinder.viewmodels

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import us.huseli.retaintheme.extensions.listItemsBetween
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.combos.TrackCombo

abstract class AbstractSelectViewModel(
    private val selectionKey: String,
    private val repos: Repositories,
) : AbstractBaseViewModel(repos) {
    val selectedAlbums: StateFlow<List<Album>> = repos.album.flowSelectedAlbums(selectionKey)
    val selectedTrackCombos: StateFlow<List<TrackCombo>> = repos.track.flowSelectedTrackCombos(selectionKey)

    val latestSelectedTrackCombo = selectedTrackCombos.map { it.lastOrNull() }
    val latestSelectedAlbum = selectedAlbums.map { it.lastOrNull() }

    fun selectAlbums(albums: Iterable<Album>) = repos.album.selectAlbums(selectionKey, albums)

    fun selectAlbumsFromLastSelected(to: Album, allAlbums: List<Album>) {
        val albums = selectedAlbums.value.lastOrNull()
            ?.let { allAlbums.listItemsBetween(it, to).plus(to) }
            ?: listOf(to)

        repos.album.selectAlbums(selectionKey, albums)
    }

    fun selectTrackCombos(combos: Iterable<TrackCombo>) = repos.track.selectTrackCombos(selectionKey, combos)

    fun selectTrackCombosFromLastSelected(to: TrackCombo, allCombos: List<TrackCombo>) {
        val combos = selectedTrackCombos.value.lastOrNull()
            ?.let { allCombos.listItemsBetween(it, to).plus(to) }
            ?: listOf(to)

        repos.track.selectTrackCombos(selectionKey, combos)
    }

    fun toggleSelected(album: Album) = repos.album.toggleAlbumSelected(selectionKey, album)

    fun toggleSelected(trackCombo: TrackCombo) {
        repos.track.toggleTrackComboSelected(selectionKey, trackCombo)
    }

    fun unselectAllAlbums() = repos.album.unselectAllAlbums(selectionKey)

    open fun unselectAllTrackCombos() = repos.track.unselectAllTrackCombos(selectionKey)
}
