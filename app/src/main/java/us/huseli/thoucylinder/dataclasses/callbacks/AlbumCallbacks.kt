package us.huseli.thoucylinder.dataclasses.callbacks

import android.content.Context
import androidx.compose.ui.platform.AndroidUriHandler
import androidx.compose.ui.platform.UriHandler
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import java.util.UUID

data class AlbumCallbacks(
    private val combo: AbstractAlbumCombo,
    private val appCallbacks: AppCallbacks,
    private val context: Context,
    private val uriHandler: UriHandler = AndroidUriHandler(context),
    val onAddToLibraryClick: () -> Unit = { appCallbacks.onAddAlbumToLibraryClick(combo) },
    val onAddToPlaylistClick: () -> Unit = { appCallbacks.onAddToPlaylistClick(Selection(album = combo.album)) },
    val onAlbumClick: (() -> Unit)? = null,
    val onAlbumLongClick: (() -> Unit)? = null,
    val onArtistClick: (UUID) -> Unit = appCallbacks.onArtistClick,
    val onCancelDownloadClick: () -> Unit = { appCallbacks.onCancelAlbumDownloadClick(combo.album.albumId) },
    val onDeleteClick: () -> Unit = { appCallbacks.onDeleteAlbumCombosClick(listOf(combo)) },
    val onDownloadClick: () -> Unit = { appCallbacks.onDownloadAlbumClick(combo.album) },
    val onEditClick: () -> Unit = { appCallbacks.onEditAlbumClick(combo) },
    val onEnqueueClick: (() -> Unit)? = null,
    val onPlayClick: (() -> Unit)? = null,
    val onPlayOnSpotifyClick: (() -> Unit)? = combo.album.spotifyWebUrl?.let { { uriHandler.openUri(it) } },
    val onPlayOnYoutubeClick: (() -> Unit)? = combo.album.youtubeWebUrl?.let { { uriHandler.openUri(it) } },
    val onStartAlbumRadioClick: () -> Unit = { appCallbacks.onStartAlbumRadioClick(combo.album.albumId) },
)
