package us.huseli.thoucylinder.dataclasses.callbacks

import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.Track
import java.util.UUID

data class AppCallbacks(
    val onAddAlbumToLibraryClick: (AbstractAlbumCombo) -> Unit,
    val onAddToPlaylistClick: (Selection) -> Unit,
    val onAlbumClick: (UUID) -> Unit,
    val onArtistClick: (String) -> Unit,
    val onBackClick: () -> Unit,
    val onCancelAlbumDownloadClick: (UUID) -> Unit,
    val onCreatePlaylistClick: () -> Unit,
    val onDeleteAlbumComboClick: (AbstractAlbumCombo) -> Unit,
    val onDeletePlaylistClick: (Playlist) -> Unit,
    val onDownloadAlbumClick: (Album) -> Unit,
    val onDownloadTrackClick: (Track) -> Unit,
    val onEditAlbumClick: (AbstractAlbumCombo) -> Unit,
    val onEditTrackClick: (Track) -> Unit,
    val onPlaylistClick: (UUID) -> Unit,
    val onShowTrackInfoClick: (AbstractTrackCombo) -> Unit,
)
