package us.huseli.thoucylinder.dataclasses.callbacks

import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistPojo
import java.util.UUID

data class AppCallbacks(
    val onAddAlbumToLibraryClick: (Album) -> Unit,
    val onAddToPlaylistClick: (Selection) -> Unit,
    val onAlbumClick: (UUID) -> Unit,
    val onArtistClick: (String) -> Unit,
    val onBackClick: () -> Unit,
    val onCancelAlbumDownloadClick: (UUID) -> Unit,
    val onCreatePlaylistClick: () -> Unit,
    val onDeletePlaylistClick: (PlaylistPojo) -> Unit,
    val onDownloadAlbumClick: (Album) -> Unit,
    val onDownloadTrackClick: (Track) -> Unit,
    val onEditAlbumClick: (Album) -> Unit,
    val onPlaylistClick: (UUID) -> Unit,
    val onShowTrackInfoClick: (AbstractTrackPojo) -> Unit,
    val onDeleteAlbumClick: (Album) -> Unit,
)
