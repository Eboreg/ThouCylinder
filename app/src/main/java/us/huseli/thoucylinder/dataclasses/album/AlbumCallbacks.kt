package us.huseli.thoucylinder.dataclasses.album

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AlbumCallbacks(
    val onAddToLibraryClick: (String) -> Unit = {},
    val onAddToPlaylistClick: (String) -> Unit = {},
    val onGotoAlbumClick: (String) -> Unit = {},
    val onArtistClick: (artistId: String) -> Unit = {},
    val onCancelDownloadClick: (String) -> Unit = {},
    val onDeleteClick: (String) -> Unit = {},
    val onDownloadClick: (String) -> Unit = {},
    val onEditClick: (String) -> Unit = {},
    val onEnqueueClick: (String) -> Unit = {},
    val onPlayClick: (String) -> Unit = {},
    val onStartRadioClick: (String) -> Unit = {},
) {
    fun withPreHook(scope: CoroutineScope, hook: suspend (String) -> String): AlbumCallbacks = copy(
        onAddToLibraryClick = { scope.launch { withContext(Dispatchers.Main) { onAddToLibraryClick(hook(it)) } } },
        onAddToPlaylistClick = { scope.launch { withContext(Dispatchers.Main) { onAddToPlaylistClick(hook(it)) } } },
        onGotoAlbumClick = { scope.launch { withContext(Dispatchers.Main) { onGotoAlbumClick(hook(it)) } } },
        onArtistClick = onArtistClick,
        onCancelDownloadClick = onCancelDownloadClick,
        onDeleteClick = { scope.launch { withContext(Dispatchers.Main) { onDeleteClick(hook(it)) } } },
        onDownloadClick = { scope.launch { withContext(Dispatchers.Main) { onDownloadClick(hook(it)) } } },
        onEditClick = { scope.launch { withContext(Dispatchers.Main) { onEditClick(hook(it)) } } },
        onEnqueueClick = { scope.launch { withContext(Dispatchers.Main) { onEnqueueClick(hook(it)) } } },
        onPlayClick = { scope.launch { withContext(Dispatchers.Main) { onPlayClick(hook(it)) } } },
        onStartRadioClick = { scope.launch { withContext(Dispatchers.Main) { onStartRadioClick(hook(it)) } } },
    )
}

val LocalAlbumCallbacks = staticCompositionLocalOf { AlbumCallbacks() }
