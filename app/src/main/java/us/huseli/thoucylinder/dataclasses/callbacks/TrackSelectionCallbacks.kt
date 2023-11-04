package us.huseli.thoucylinder.dataclasses.callbacks

data class TrackSelectionCallbacks(
    val onAddToPlaylistClick: () -> Unit,
    val onPlayClick: () -> Unit,
    val onPlayNextClick: () -> Unit,
    val onUnselectAllClick: () -> Unit,
)
