package us.huseli.thoucylinder.dataclasses.callbacks

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf

@Stable
data class AppCallbacks(
    val onBackClick: () -> Unit = {},
    val onGotoArtistClick: (String) -> Unit = {},
    val onGotoImportClick: () -> Unit = {},
    val onGotoLibraryClick: () -> Unit = {},
    val onGotoPlaylistClick: (String) -> Unit = {},
    val onGotoSearchClick: () -> Unit = {},
    val onGotoSettingsClick: () -> Unit = {},
)

val LocalAppCallbacks = staticCompositionLocalOf { AppCallbacks() }
