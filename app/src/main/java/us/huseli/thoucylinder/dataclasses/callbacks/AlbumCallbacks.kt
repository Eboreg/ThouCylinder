package us.huseli.thoucylinder.dataclasses.callbacks

import android.content.Context
import androidx.compose.ui.platform.AndroidUriHandler
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo

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
    val onPlayOnYoutubeClick: (() -> Unit)? = null,
    val onPlayOnSpotifyClick: (() -> Unit)? = null,
) {
    companion object {
        fun fromAppCallbacks(
            pojo: AbstractAlbumPojo,
            appCallbacks: AppCallbacks,
            context: Context,
            onAddToPlaylistClick: (() -> Unit)? = null,
            onAlbumClick: (() -> Unit)? = null,
            onAlbumLongClick: (() -> Unit)? = null,
            onPlayClick: () -> Unit,
            onEnqueueClick: () -> Unit,
            onRemoveFromLibraryClick: (() -> Unit)? = null,
        ): AlbumCallbacks {
            val uriHandler = AndroidUriHandler(context)

            return AlbumCallbacks(
                onAddToLibraryClick = { appCallbacks.onAddAlbumToLibraryClick(pojo.album) },
                onAddToPlaylistClick = onAddToPlaylistClick
                    ?: { appCallbacks.onAddToPlaylistClick(Selection(album = pojo.album)) },
                onAlbumClick = onAlbumClick,
                onAlbumLongClick = onAlbumLongClick,
                onArtistClick = pojo.album.artist?.let { { appCallbacks.onArtistClick(it) } },
                onCancelDownloadClick = { appCallbacks.onCancelAlbumDownloadClick(pojo.album.albumId) },
                onDownloadClick = { appCallbacks.onDownloadAlbumClick(pojo.album) },
                onEditClick = { appCallbacks.onEditAlbumClick(pojo.album) },
                onPlayClick = onPlayClick,
                onEnqueueClick = onEnqueueClick,
                onRemoveFromLibraryClick = onRemoveFromLibraryClick
                    ?: { appCallbacks.onRemoveAlbumFromLibraryClick(pojo.album) },
                onDeleteClick = { appCallbacks.onDeleteAlbumClick(pojo.album) },
                onPlayOnYoutubeClick = pojo.album.youtubeWebUrl?.let { { uriHandler.openUri(it) } },
                onPlayOnSpotifyClick = pojo.spotifyWebUrl?.let { { uriHandler.openUri(it) } },
            )
        }
    }
}
