package us.huseli.thoucylinder.dataclasses.callbacks

data class TrackSelectionCallbacks(
    val onAddToPlaylistClick: () -> Unit,
    val onPlayClick: (() -> Unit)? = null,
    val onEnqueueClick: (() -> Unit)? = null,
    val onUnselectAllClick: () -> Unit,
    val onSelectAllClick: (() -> Unit)? = null,
)
