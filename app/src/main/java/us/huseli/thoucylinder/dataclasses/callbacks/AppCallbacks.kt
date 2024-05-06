package us.huseli.thoucylinder.dataclasses.callbacks

import androidx.compose.runtime.Stable

@Stable
data class AppDialogCallbacks(
    val onAddAlbumsToPlaylistClick: (Collection<String>) -> Unit,
    val onAddTracksToPlaylistClick: (Collection<String>) -> Unit,
    val onCreatePlaylistClick: () -> Unit,
    val onDeleteAlbumsClick: (Collection<String>) -> Unit,
    val onDownloadAlbumClick: (String) -> Unit,
    val onEditAlbumClick: (String) -> Unit,
    val onEditTrackClick: (String) -> Unit,
    val onRadioClick: () -> Unit,
    val onShowTrackInfoClick: (String) -> Unit,
)

@Stable
data class AppCallbacks(
    val onAddAlbumsToPlaylistClick: (Collection<String>) -> Unit,
    val onAddTracksToPlaylistClick: (Collection<String>) -> Unit,
    val onArtistClick: (String) -> Unit,
    val onBackClick: () -> Unit,
    val onCreatePlaylistClick: () -> Unit,
    val onDeleteAlbumsClick: (Collection<String>) -> Unit,
    val onDeletePlaylistClick: (String) -> Unit,
    val onPlaylistClick: (String) -> Unit,
    val onStartArtistRadioClick: (String) -> Unit,
)
