package us.huseli.thoucylinder.dataclasses.callbacks

import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackPojo

class TrackCallbacks(
    val onAddToPlaylistClick: () -> Unit,
    val onAlbumClick: (() -> Unit)? = null,
    val onArtistClick: (() -> Unit)? = null,
    val onDownloadClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null,
    val onPlayNextClick: (() -> Unit)? = null,
    val onShowInfoClick: () -> Unit,
    val onTrackClick: (() -> Unit)? = null,
) {
    companion object {
        fun <T : AbstractTrackPojo> fromAppCallbacks(
            appCallbacks: AppCallbacks,
            pojo: T,
            onAddToPlaylistClick: (() -> Unit)? = null,
            onAlbumClick: (() -> Unit)? = null,
            onArtistClick: (() -> Unit)? = null,
            onLongClick: (() -> Unit)? = null,
            onPlayNextClick: (() -> Unit)? = null,
            onTrackClick: (() -> Unit)? = null,
        ) = TrackCallbacks(
            onAddToPlaylistClick = onAddToPlaylistClick
                ?: { appCallbacks.onAddToPlaylistClick(Selection(trackPojo = pojo)) },
            onAlbumClick = onAlbumClick ?: pojo.album?.let { { appCallbacks.onAlbumClick(it.albumId) } },
            onArtistClick = onArtistClick ?: pojo.artist?.let { { appCallbacks.onArtistClick(it) } },
            onDownloadClick = { appCallbacks.onDownloadTrackClick(pojo.track) },
            onLongClick = onLongClick,
            onPlayNextClick = onPlayNextClick,
            onShowInfoClick = { appCallbacks.onShowTrackInfoClick(pojo) },
            onTrackClick = onTrackClick,
        )
    }
}
