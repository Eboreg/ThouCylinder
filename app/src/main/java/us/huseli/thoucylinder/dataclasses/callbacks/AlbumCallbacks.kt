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
    val onPlayNextClick: () -> Unit,
    val onRemoveFromLibraryClick: () -> Unit,
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
            onPlayNextClick: () -> Unit,
            onRemoveFromLibraryClick: () -> Unit,
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
            onPlayNextClick = onPlayNextClick,
            onRemoveFromLibraryClick = onRemoveFromLibraryClick,
        )
    }
}
