package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.umlautify

abstract class AbstractAlbumListViewModel(
    selectionKey: String,
    private val repos: Repositories,
) : AbstractTrackListViewModel(selectionKey, repos) {
    val albumDownloadTasks = repos.youtube.albumDownloadTasks

    fun enqueueAlbum(album: Album, context: Context) = enqueueAlbums(listOf(album), context)

    fun enqueueAlbums(albums: List<Album>, context: Context) = viewModelScope.launch {
        val combos =
            getQueueTrackCombos(repos.album.listAlbumTrackCombos(albums.map { it.albumId }), repos.player.nextItemIndex)

        if (combos.isNotEmpty()) {
            repos.player.insertNext(combos)
            SnackbarEngine.addInfo(
                context.resources
                    .getQuantityString(R.plurals.x_albums_enqueued_next, albums.size, albums.size)
                    .umlautify()
            )
        }
    }

    fun playAlbum(album: Album) = playAlbums(listOf(album))

    fun playAlbums(albums: List<Album>) = viewModelScope.launch {
        repos.player.replaceAndPlay(
            getQueueTrackCombos(repos.album.listAlbumTrackCombos(albums.map { it.albumId }))
        )
    }
}
