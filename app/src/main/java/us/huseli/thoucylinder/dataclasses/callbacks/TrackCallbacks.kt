package us.huseli.thoucylinder.dataclasses.callbacks

import android.content.Context
import androidx.compose.ui.platform.AndroidUriHandler
import androidx.compose.ui.platform.UriHandler
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import java.util.UUID

data class TrackCallbacks<out T : AbstractTrackCombo>(
    private val appCallbacks: AppCallbacks,
    private val combo: T,
    private val context: Context,
    private val uriHandler: UriHandler = AndroidUriHandler(context),
    val onAddToPlaylistClick: () -> Unit = { appCallbacks.onAddToPlaylistClick(Selection(track = combo.track)) },
    val onAlbumClick: (() -> Unit)? = combo.track.albumId?.let { { appCallbacks.onAlbumClick(it) } },
    val onArtistClick: (UUID) -> Unit = appCallbacks.onArtistClick,
    val onDownloadClick: () -> Unit = { appCallbacks.onDownloadTrackClick(combo) },
    val onLongClick: (() -> Unit)? = null,
    val onEnqueueClick: (() -> Unit)? = null,
    val onShowInfoClick: () -> Unit = { appCallbacks.onShowTrackInfoClick(combo) },
    val onTrackClick: (() -> Unit)? = null,
    val onPlayOnYoutubeClick: (() -> Unit)? = combo.track.youtubeWebUrl?.let { { uriHandler.openUri(it) } },
    val onPlayOnSpotifyClick: (() -> Unit)? = combo.track.spotifyWebUrl?.let { { uriHandler.openUri(it) } },
    val onEach: (() -> Unit)? = null,
    val onEditTrackClick: () -> Unit = { appCallbacks.onEditTrackClick(combo) },
    val onStartTrackRadioClick: () -> Unit = { appCallbacks.onStartTrackRadioClick(combo.track.trackId) },
)
