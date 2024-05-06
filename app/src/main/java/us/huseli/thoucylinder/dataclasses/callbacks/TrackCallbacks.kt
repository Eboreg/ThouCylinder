package us.huseli.thoucylinder.dataclasses.callbacks

data class TrackCallbacks(
    val onAddToPlaylistClick: (String) -> Unit,
    val onDownloadClick: (String) -> Unit,
    val onEditClick: (String) -> Unit,
    val onEnqueueClick: (String) -> Unit,
    val onGotoAlbumClick: (String) -> Unit,
    val onGotoArtistClick: (String) -> Unit,
    val onShowInfoClick: (String) -> Unit,
    val onStartRadioClick: (String) -> Unit,
    val onTrackClick: (String) -> Unit,
    val onEach: (String) -> Unit = {},
    val onLongClick: (String) -> Unit = {},
)
