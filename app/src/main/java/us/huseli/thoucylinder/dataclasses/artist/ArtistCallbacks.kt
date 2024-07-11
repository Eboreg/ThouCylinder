package us.huseli.thoucylinder.dataclasses.artist

import androidx.compose.runtime.staticCompositionLocalOf

data class ArtistCallbacks(
    val onPlayClick: (String) -> Unit = {},
    val onStartRadioClick: (String) -> Unit = {},
    val onEnqueueClick: (String) -> Unit = {},
    val onAddToPlaylistClick: (String) -> Unit = {},
    val onGotoArtistClick: (String) -> Unit = {},
)

val LocalArtistCallbacks = staticCompositionLocalOf { ArtistCallbacks() }
