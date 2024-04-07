package us.huseli.thoucylinder.dataclasses.callbacks

import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.Track

data class AppCallbacks(
    val onAddAlbumToLibraryClick: (Album.ViewState) -> Unit,
    val onAddToPlaylistClick: (Selection) -> Unit,
    val onAlbumClick: (String) -> Unit,
    val onArtistClick: (String) -> Unit,
    val onBackClick: () -> Unit,
    val onCancelAlbumDownloadClick: (String) -> Unit,
    val onCreatePlaylistClick: () -> Unit,
    val onDeleteAlbumsClick: (Collection<Album.ViewState>) -> Unit,
    val onDeletePlaylistClick: (Playlist) -> Unit,
    val onDownloadAlbumClick: (Album) -> Unit,
    val onDownloadTrackClick: (Track.ViewState) -> Unit,
    val onEditAlbumClick: (Album.ViewState) -> Unit,
    val onEditTrackClick: (Track.ViewState) -> Unit,
    val onPlaylistClick: (String) -> Unit,
    val onShowTrackInfoClick: (Track) -> Unit,
    val onStartAlbumRadioClick: (String) -> Unit,
    val onStartArtistRadioClick: (String) -> Unit,
    val onStartTrackRadioClick: (String) -> Unit,
)
