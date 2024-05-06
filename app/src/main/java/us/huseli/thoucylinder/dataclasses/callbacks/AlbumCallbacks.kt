package us.huseli.thoucylinder.dataclasses.callbacks


data class AlbumCallbacks(
    val onAddToLibraryClick: (String) -> Unit,
    val onAddToPlaylistClick: (String) -> Unit,
    val onAlbumClick: ((String) -> Unit)? = null,
    val onAlbumLongClick: ((String) -> Unit)? = null,
    val onArtistClick: (String) -> Unit,
    val onCancelDownloadClick: (String) -> Unit,
    val onDeleteClick: (String) -> Unit,
    val onDownloadClick: (String) -> Unit,
    val onEditClick: (String) -> Unit,
    val onEnqueueClick: ((String) -> Unit)? = null,
    val onPlayClick: ((String) -> Unit)? = null,
    val onStartRadioClick: (String) -> Unit,
)
