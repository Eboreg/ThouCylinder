package us.huseli.thoucylinder.dataclasses.callbacks

data class AlbumSelectionCallbacks(
    val onAddToPlaylistClick: () -> Unit,
    val onPlayClick: () -> Unit,
    val onEnqueueClick: () -> Unit,
    val onSelectAllClick: () -> Unit,
    val onUnselectAllClick: () -> Unit,
    val onDeleteClick: (() -> Unit)? = null,
)
