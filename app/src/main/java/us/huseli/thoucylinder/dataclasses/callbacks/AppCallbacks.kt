package us.huseli.thoucylinder.dataclasses.callbacks

import androidx.compose.runtime.Composable
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistPojo
import java.util.UUID

data class AppCallbacks(
    val onAddAlbumToLibraryClick: (AbstractAlbumPojo) -> Unit,
    val onAddToPlaylistClick: (Selection) -> Unit,
    val onAlbumClick: (UUID) -> Unit,
    val onArtistClick: (String) -> Unit,
    val onBackClick: () -> Unit,
    val onCancelAlbumDownloadClick: (UUID) -> Unit,
    val onCreatePlaylistClick: () -> Unit,
    val onDeleteAlbumPojoClick: (AbstractAlbumPojo) -> Unit,
    val onDeletePlaylistClick: (PlaylistPojo) -> Unit,
    val onDownloadAlbumClick: (Album) -> Unit,
    val onDownloadTrackClick: (Track) -> Unit,
    val onEditAlbumClick: (AbstractAlbumPojo) -> Unit,
    val onEditTrackClick: (Track) -> Unit,
    val onPlaylistClick: (UUID) -> Unit,
    val onShowTrackInfoClick: (AbstractTrackPojo) -> Unit,
)
