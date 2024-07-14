package us.huseli.thoucylinder.compose.artist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.asIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import us.huseli.thoucylinder.compose.ListSettingsRow
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.compose.track.TrackCollection
import us.huseli.thoucylinder.compose.utils.BasicHeader
import us.huseli.thoucylinder.compose.utils.Toolbar
import us.huseli.thoucylinder.dataclasses.album.LocalAlbumCallbacks
import us.huseli.thoucylinder.dataclasses.artist.LocalArtistCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.LocalAppDialogCallbacks
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.ArtistViewModel

@Composable
fun ArtistScreen(modifier: Modifier = Modifier, viewModel: ArtistViewModel = hiltViewModel()) {
    val artistCallbacks = LocalArtistCallbacks.current
    val dialogCallbacks = LocalAppDialogCallbacks.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val displayType by viewModel.displayType.collectAsStateWithLifecycle()
    val listType by viewModel.listType.collectAsStateWithLifecycle()
    val relatedArtists by viewModel.relatedArtists.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxWidth()) {
        uiState?.also {
            BasicHeader(title = it.name.umlautify())
        }

        Toolbar {
            ListSettingsRow(
                currentDisplayType = displayType,
                currentListType = listType,
                onDisplayTypeChange = { viewModel.setDisplayType(it) },
                onListTypeChange = { viewModel.setListType(it) },
                excludeListTypes = persistentListOf(ListType.ARTISTS, ListType.PLAYLISTS),
            )
        }

        when (listType) {
            ListType.ALBUMS -> {
                val selectedAlbumCount = viewModel.selectedAlbumCount.collectAsStateWithLifecycle().asIntState()
                val uiStates by viewModel.albumUiStates.collectAsStateWithLifecycle()
                val albumSelectionCallbacks = viewModel.getAlbumSelectionCallbacks(dialogCallbacks)
                val isLoadingAlbums by viewModel.isLoadingAlbums.collectAsStateWithLifecycle()
                val albumCallbacks = LocalAlbumCallbacks.current
                val otherAlbums by viewModel.otherAlbums.collectAsStateWithLifecycle()
                val otherAlbumsPreview by viewModel.otherAlbumsPreview.collectAsStateWithLifecycle()
                val otherAlbumTypes by viewModel.otherAlbumTypes.collectAsStateWithLifecycle()

                ArtistAlbumCollection(
                    uiStates = uiStates,
                    selectionCallbacks = albumSelectionCallbacks,
                    displayType = displayType,
                    isLoading = isLoadingAlbums,
                    selectedAlbumCount = { selectedAlbumCount.intValue },
                    downloadStateFlow = { viewModel.getAlbumDownloadUiStateFlow(it) },
                    relatedArtists = relatedArtists,
                    onClick = remember {
                        { viewModel.onAlbumClick(it.albumId, albumCallbacks.onGotoAlbumClick) }
                    },
                    onLongClick = remember { { viewModel.onAlbumLongClick(it.albumId) } },
                    onRelatedArtistClick = { viewModel.onRelatedArtistClick(it, artistCallbacks.onGotoArtistClick) },
                    otherAlbums = otherAlbums,
                    otherAlbumsPreview = otherAlbumsPreview,
                    otherAlbumTypes = otherAlbumTypes,
                    onOtherAlbumClick = { viewModel.onOtherAlbumClick(it, albumCallbacks.onGotoAlbumClick) },
                    onOtherAlbumTypeClick = { viewModel.toggleOtherAlbumsType(it) },
                )
            }
            ListType.TRACKS -> {
                val selectedTrackCount = viewModel.selectedTrackCount.collectAsStateWithLifecycle().asIntState()
                val trackUiStates by viewModel.trackUiStates.collectAsStateWithLifecycle()
                val isLoadingTracks by viewModel.isLoadingTracks.collectAsStateWithLifecycle()
                val trackSelectionCallbacks = viewModel.getTrackSelectionCallbacks(dialogCallbacks)

                TrackCollection(
                    states = { trackUiStates },
                    displayType = displayType,
                    getDownloadStateFlow = { viewModel.getTrackDownloadUiStateFlow(it) },
                    onClick = { viewModel.onTrackClick(it) },
                    onLongClick = { viewModel.onTrackLongClick(it.id) },
                    selectedTrackCount = { selectedTrackCount.intValue },
                    trackSelectionCallbacks = trackSelectionCallbacks,
                    isLoading = isLoadingTracks,
                    showAlbum = true,
                    showArtist = false,
                    trailingContent = {
                        RelatedArtistsCardList(
                            relatedArtists = relatedArtists,
                            onClick = { viewModel.onRelatedArtistClick(it, artistCallbacks.onGotoArtistClick) },
                        )
                    },
                )
            }
            else -> {}
        }
    }
}
