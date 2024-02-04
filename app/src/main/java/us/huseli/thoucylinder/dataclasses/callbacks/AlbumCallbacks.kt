package us.huseli.thoucylinder.dataclasses.callbacks

import android.content.Context
import androidx.compose.ui.platform.AndroidUriHandler
import androidx.compose.ui.platform.UriHandler
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo

data class AlbumCallbacks(
    private val pojo: AbstractAlbumPojo,
    private val appCallbacks: AppCallbacks,
    private val context: Context,
    private val uriHandler: UriHandler = AndroidUriHandler(context),
    val onAddToLibraryClick: () -> Unit = { appCallbacks.onAddAlbumToLibraryClick(pojo) },
    val onAddToPlaylistClick: () -> Unit = { appCallbacks.onAddToPlaylistClick(Selection(album = pojo.album)) },
    val onAlbumClick: (() -> Unit)? = null,
    val onAlbumLongClick: (() -> Unit)? = null,
    val onArtistClick: (() -> Unit)? = pojo.album.artist?.let { { appCallbacks.onArtistClick(it) } },
    val onCancelDownloadClick: () -> Unit = { appCallbacks.onCancelAlbumDownloadClick(pojo.album.albumId) },
    val onDeleteClick: () -> Unit = { appCallbacks.onDeleteAlbumPojoClick(pojo) },
    val onDownloadClick: () -> Unit = { appCallbacks.onDownloadAlbumClick(pojo.album) },
    val onEditClick: () -> Unit = { appCallbacks.onEditAlbumClick(pojo) },
    val onEnqueueClick: () -> Unit,
    val onPlayClick: () -> Unit,
    val onPlayOnSpotifyClick: (() -> Unit)? = pojo.spotifyWebUrl?.let { { uriHandler.openUri(it) } },
    val onPlayOnYoutubeClick: (() -> Unit)? = pojo.album.youtubeWebUrl?.let { { uriHandler.openUri(it) } },
)
