package us.huseli.thoucylinder.dataclasses.album

data class AlbumSelectionCallbacks(
    val onAddToPlaylistClick: () -> Unit,
    val onDeleteClick: (() -> Unit)? = null,
    val onEnqueueClick: () -> Unit,
    val onExportClick: () -> Unit,
    val onPlayClick: () -> Unit,
    val onSelectAllClick: () -> Unit,
    val onUnselectAllClick: () -> Unit,
)
