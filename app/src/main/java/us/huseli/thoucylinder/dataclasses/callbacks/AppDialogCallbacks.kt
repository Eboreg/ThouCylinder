package us.huseli.thoucylinder.dataclasses.callbacks

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf

@Stable
data class AppDialogCallbacks(
    val onAddAlbumsToPlaylistClick: (Collection<String>) -> Unit = {},
    val onAddArtistToPlaylistClick: (String) -> Unit = {},
    val onAddTracksToPlaylistClick: (Collection<String>) -> Unit = {},
    val onCreatePlaylistClick: () -> Unit = {},
    val onDeleteAlbumsClick: (Collection<String>) -> Unit = {},
    val onDownloadAlbumClick: (String) -> Unit = {},
    val onEditAlbumClick: (String) -> Unit = {},
    val onEditTrackClick: (String) -> Unit = {},
    val onExportAlbumsClick: (Collection<String>) -> Unit = {},
    val onExportAllTracksClick: () -> Unit = {},
    val onExportPlaylistClick: (String) -> Unit = {},
    val onExportTracksClick: (Collection<String>) -> Unit = {},
    val onRadioClick: () -> Unit = {},
    val onShowTrackInfoClick: (String) -> Unit = {},
)

val LocalAppDialogCallbacks = staticCompositionLocalOf { AppDialogCallbacks() }
