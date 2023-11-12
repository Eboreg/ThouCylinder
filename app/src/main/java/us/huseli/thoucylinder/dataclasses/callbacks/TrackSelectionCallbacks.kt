package us.huseli.thoucylinder.dataclasses.callbacks

data class TrackSelectionCallbacks(
    val onAddToPlaylistClick: () -> Unit,
    val onPlayClick: () -> Unit,
    val onEnqueueClick: () -> Unit,
    val onUnselectAllClick: () -> Unit,
)
