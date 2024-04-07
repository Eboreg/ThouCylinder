package us.huseli.thoucylinder.dataclasses.callbacks

import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.entities.Track

data class TrackCallbacks(
    private val appCallbacks: AppCallbacks,
    private val state: Track.ViewState,
    val onAddToPlaylistClick: () -> Unit = { appCallbacks.onAddToPlaylistClick(Selection(track = state.track)) },
    val onAlbumClick: (() -> Unit)? = state.track.albumId?.let { { appCallbacks.onAlbumClick(it) } },
    val onArtistClick: (String) -> Unit = appCallbacks.onArtistClick,
    val onDownloadClick: () -> Unit = { appCallbacks.onDownloadTrackClick(state) },
    val onLongClick: (() -> Unit)? = null,
    val onEnqueueClick: (() -> Unit)? = null,
    val onShowInfoClick: () -> Unit = { appCallbacks.onShowTrackInfoClick(state.track) },
    val onTrackClick: (() -> Unit)? = null,
    val onEach: (() -> Unit)? = null,
    val onEditTrackClick: () -> Unit = { appCallbacks.onEditTrackClick(state) },
    val onStartTrackRadioClick: () -> Unit = { appCallbacks.onStartTrackRadioClick(state.track.trackId) },
)
