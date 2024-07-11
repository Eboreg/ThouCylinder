package us.huseli.thoucylinder.dataclasses.track

import androidx.compose.runtime.staticCompositionLocalOf

data class TrackCallbacks(
    val onAddToPlaylistClick: (AbstractTrackUiState) -> Unit = {},
    val onDownloadClick: (AbstractTrackUiState) -> Unit = {},
    val onEditClick: (AbstractTrackUiState) -> Unit = {},
    val onEnqueueClick: ((AbstractTrackUiState) -> Unit)? = {},
    val onGotoAlbumClick: (String) -> Unit = {},
    val onGotoArtistClick: (String) -> Unit = {},
    val onPlayClick: ((AbstractTrackUiState) -> Unit)? = {},
    val onShowInfoClick: (AbstractTrackUiState) -> Unit = {},
    val onStartRadioClick: (AbstractTrackUiState) -> Unit = {},
)

val LocalTrackCallbacks = staticCompositionLocalOf { TrackCallbacks() }
