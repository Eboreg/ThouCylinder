package us.huseli.thoucylinder.dataclasses.callbacks

import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.dataclasses.entities.Album

data class AlbumCallbacks(
    val onAddToLibraryClick: () -> Unit,
    val onAddToPlaylistClick: () -> Unit,
    val onAlbumClick: (() -> Unit)? = null,
    val onAlbumLongClick: (() -> Unit)? = null,
    val onArtistClick: (() -> Unit)? = null,
    val onCancelDownloadClick: () -> Unit,
    val onDownloadClick: () -> Unit,
    val onEditClick: () -> Unit,
    val onPlayClick: () -> Unit,
    val onEnqueueClick: () -> Unit,
    val onRemoveFromLibraryClick: () -> Unit,
    val onDeleteClick: () -> Unit,
) {
    companion object {
        fun fromAppCallbacks(
            album: Album,
            appCallbacks: AppCallbacks,
            onAddToPlaylistClick: (() -> Unit)? = null,
            onAlbumClick: (() -> Unit)? = null,
            onAlbumLongClick: (() -> Unit)? = null,
            onCancelDownloadClick: (() -> Unit)? = null,
            onPlayClick: () -> Unit,
            onEnqueueClick: () -> Unit,
            onRemoveFromLibraryClick: (() -> Unit)? = null,
            onDeleteClick: (() -> Unit)? = null,
        ) = AlbumCallbacks(
            onAddToLibraryClick = { appCallbacks.onAddAlbumToLibraryClick(album) },
            onAddToPlaylistClick = onAddToPlaylistClick
                ?: { appCallbacks.onAddToPlaylistClick(Selection(album = album)) },
            onAlbumClick = onAlbumClick ?: { appCallbacks.onAlbumClick(album.albumId) },
            onAlbumLongClick = onAlbumLongClick,
            onArtistClick = album.artist?.let { { appCallbacks.onArtistClick(it) } },
            onCancelDownloadClick = onCancelDownloadClick ?: { appCallbacks.onCancelAlbumDownloadClick(album.albumId) },
            onDownloadClick = { appCallbacks.onDownloadAlbumClick(album) },
            onEditClick = { appCallbacks.onEditAlbumClick(album) },
            onPlayClick = onPlayClick,
            onEnqueueClick = onEnqueueClick,
            onRemoveFromLibraryClick = onRemoveFromLibraryClick
                ?: { appCallbacks.onRemoveAlbumFromLibraryClick(album) },
            onDeleteClick = onDeleteClick ?: { appCallbacks.onDeleteAlbumClick(album) },
        )
    }
}
