package us.huseli.thoucylinder.dataclasses.callbacks

import android.content.Context
import androidx.compose.ui.platform.AndroidUriHandler
import androidx.compose.ui.platform.UriHandler
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackPojo

data class TrackCallbacks<out T : AbstractTrackPojo>(
    private val appCallbacks: AppCallbacks,
    private val pojo: T,
    private val context: Context,
    private val uriHandler: UriHandler = AndroidUriHandler(context),
    val onAddToPlaylistClick: () -> Unit = { appCallbacks.onAddToPlaylistClick(Selection(track = pojo.track)) },
    val onAlbumClick: (() -> Unit)? = pojo.album?.let { { appCallbacks.onAlbumClick(it.albumId) } },
    val onArtistClick: (() -> Unit)? = pojo.artist?.let { { appCallbacks.onArtistClick(it) } },
    val onDownloadClick: () -> Unit = { appCallbacks.onDownloadTrackClick(pojo.track) },
    val onLongClick: (() -> Unit)? = null,
    val onEnqueueClick: (() -> Unit)? = null,
    val onShowInfoClick: () -> Unit = { appCallbacks.onShowTrackInfoClick(pojo) },
    val onTrackClick: (() -> Unit)? = null,
    val onPlayOnYoutubeClick: (() -> Unit)? = pojo.track.youtubeWebUrl?.let { { uriHandler.openUri(it) } },
    val onPlayOnSpotifyClick: (() -> Unit)? = pojo.spotifyWebUrl?.let { { uriHandler.openUri(it) } },
    val onEach: (() -> Unit)? = null,
    val onEditTrackClick: () -> Unit = { appCallbacks.onEditTrackClick(pojo.track) },
)
