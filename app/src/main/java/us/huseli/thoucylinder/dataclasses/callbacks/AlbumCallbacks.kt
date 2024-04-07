package us.huseli.thoucylinder.dataclasses.callbacks

import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.entities.Album

data class AlbumCallbacks(
    private val state: Album.ViewState,
    private val appCallbacks: AppCallbacks,
    val onAddToLibraryClick: () -> Unit = { appCallbacks.onAddAlbumToLibraryClick(state) },
    val onAddToPlaylistClick: () -> Unit = { appCallbacks.onAddToPlaylistClick(Selection(album = state.album)) },
    val onAlbumClick: (() -> Unit)? = null,
    val onAlbumLongClick: (() -> Unit)? = null,
    val onArtistClick: (String) -> Unit = appCallbacks.onArtistClick,
    val onCancelDownloadClick: () -> Unit = { appCallbacks.onCancelAlbumDownloadClick(state.album.albumId) },
    val onDeleteClick: () -> Unit = { appCallbacks.onDeleteAlbumsClick(listOf(state)) },
    val onDownloadClick: () -> Unit = { appCallbacks.onDownloadAlbumClick(state.album) },
    val onEditClick: () -> Unit = { appCallbacks.onEditAlbumClick(state) },
    val onEnqueueClick: (() -> Unit)? = null,
    val onPlayClick: (() -> Unit)? = null,
    val onStartAlbumRadioClick: () -> Unit = { appCallbacks.onStartAlbumRadioClick(state.album.albumId) },
)
