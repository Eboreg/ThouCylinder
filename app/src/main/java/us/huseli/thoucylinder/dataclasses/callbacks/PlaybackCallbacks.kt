package us.huseli.thoucylinder.dataclasses.callbacks

data class PlaybackCallbacks(
    val playOrPauseCurrent: () -> Unit,
    val seekToProgress: (Float) -> Unit,
    val skipToNext: () -> Unit,
    val skipToPrevious: () -> Unit,
    val skipToStartOrPrevious: () -> Unit,
    val toggleRepeat: () -> Unit,
    val toggleShuffle: () -> Unit,
)
