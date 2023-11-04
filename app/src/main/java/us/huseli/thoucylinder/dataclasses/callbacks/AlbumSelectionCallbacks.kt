package us.huseli.thoucylinder.dataclasses.callbacks

import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.dataclasses.entities.Album

data class AlbumSelectionCallbacks(
    val onAddToPlaylistClick: () -> Unit,
    val onPlayClick: () -> Unit,
    val onPlayNextClick: () -> Unit,
    val onSelectAllClick: (() -> Unit)? = null,
    val onUnselectAllClick: () -> Unit,
) {
    constructor(
        albums: List<Album>,
        appCallbacks: AppCallbacks,
        onPlayClick: () -> Unit,
        onPlayNextClick: () -> Unit,
        onSelectAllClick: (() -> Unit)? = null,
        onUnselectAllClick: () -> Unit,
    ) : this(
        onAddToPlaylistClick = { appCallbacks.onAddToPlaylistClick(Selection(albums = albums)) },
        onPlayClick = onPlayClick,
        onPlayNextClick = onPlayNextClick,
        onSelectAllClick = onSelectAllClick,
        onUnselectAllClick = onUnselectAllClick,
    )
}
