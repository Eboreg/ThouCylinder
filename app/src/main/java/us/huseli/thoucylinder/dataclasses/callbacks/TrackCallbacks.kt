package us.huseli.thoucylinder.dataclasses.callbacks

import android.content.Context
import androidx.compose.ui.platform.AndroidUriHandler
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackPojo

data class TrackCallbacks(
    val onAddToPlaylistClick: () -> Unit,
    val onAlbumClick: (() -> Unit)? = null,
    val onArtistClick: (() -> Unit)? = null,
    val onDownloadClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null,
    val onEnqueueClick: (() -> Unit)? = null,
    val onShowInfoClick: () -> Unit,
    val onTrackClick: (() -> Unit)? = null,
    val onPlayOnYoutubeClick: (() -> Unit)? = null,
    val onPlayOnSpotifyClick: (() -> Unit)? = null,
    val onEach: (() -> Unit)? = null,
) {
    companion object {
        fun <T : AbstractTrackPojo> fromAppCallbacks(
            appCallbacks: AppCallbacks,
            pojo: T,
            context: Context,
            onAddToPlaylistClick: (() -> Unit)? = null,
            onLongClick: (() -> Unit)? = null,
            onEnqueueClick: (() -> Unit)? = null,
            onTrackClick: (() -> Unit)? = null,
            onEach: (() -> Unit)? = null,
        ): TrackCallbacks {
            val uriHandler = AndroidUriHandler(context)

            return TrackCallbacks(
                onAddToPlaylistClick = onAddToPlaylistClick
                    ?: { appCallbacks.onAddToPlaylistClick(Selection(track = pojo.track)) },
                onAlbumClick = pojo.album?.let { { appCallbacks.onAlbumClick(it.albumId) } },
                onArtistClick = pojo.artist?.let { { appCallbacks.onArtistClick(it) } },
                onDownloadClick = { appCallbacks.onDownloadTrackClick(pojo.track) },
                onLongClick = onLongClick,
                onEnqueueClick = onEnqueueClick,
                onShowInfoClick = { appCallbacks.onShowTrackInfoClick(pojo) },
                onTrackClick = onTrackClick,
                onPlayOnYoutubeClick = pojo.track.youtubeWebUrl?.let { { uriHandler.openUri(it) } },
                onPlayOnSpotifyClick = pojo.spotifyWebUrl?.let { { uriHandler.openUri(it) } },
                onEach = onEach,
            )
        }
    }
}
