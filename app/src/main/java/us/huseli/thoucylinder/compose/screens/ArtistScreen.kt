package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material.icons.sharp.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.ArtistContextMenu
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListSettingsRow
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.compose.album.AlbumGrid
import us.huseli.thoucylinder.compose.album.AlbumList
import us.huseli.thoucylinder.compose.track.TrackGrid
import us.huseli.thoucylinder.compose.track.TrackList
import us.huseli.thoucylinder.compose.utils.CollapsibleToolbar
import us.huseli.thoucylinder.compose.utils.rememberToolbarScrollConnection
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.uistates.TrackUiState
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.ArtistViewModel

@Composable
fun ArtistScreen(
    modifier: Modifier = Modifier,
    viewModel: ArtistViewModel = hiltViewModel(),
    appCallbacks: AppCallbacks,
    albumCallbacks: AlbumCallbacks,
    trackCallbacks: TrackCallbacks,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val artist by viewModel.artist.collectAsStateWithLifecycle()
    val displayType by viewModel.displayType.collectAsStateWithLifecycle()
    val listType by viewModel.listType.collectAsStateWithLifecycle()

    var isContextMenuShown by rememberSaveable { mutableStateOf(false) }
    var showToolbars by remember { mutableStateOf(true) }

    val nestedScrollConnection = rememberToolbarScrollConnection { showToolbars = it }

    Column(modifier = modifier.fillMaxWidth()) {
        CollapsibleToolbar(show = { showToolbars }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                IconButton(
                    onClick = appCallbacks.onBackClick,
                    content = { Icon(Icons.AutoMirrored.Sharp.ArrowBack, stringResource(R.string.go_back)) }
                )
                Column(modifier = Modifier.weight(1f)) {
                    artist?.name?.also {
                        Text(
                            text = it.umlautify(),
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                artist?.also { artist ->
                    IconButton(
                        onClick = { isContextMenuShown = !isContextMenuShown },
                        modifier = Modifier.size(32.dp, 40.dp),
                        content = {
                            ArtistContextMenu(
                                expanded = isContextMenuShown,
                                onDismissRequest = { isContextMenuShown = false },
                                onPlayClick = { viewModel.playArtist() },
                                onStartRadioClick = { appCallbacks.onStartArtistRadioClick(artist.artistId) },
                                onEnqueueClick = { viewModel.enqueueArtist() },
                                onAddToPlaylistClick = {
                                    viewModel.onAllArtistTrackIds {
                                        appCallbacks.onAddTracksToPlaylistClick(it)
                                    }
                                },
                                onSpotifyClick = artist.spotifyWebUrl?.let { { uriHandler.openUri(it) } },
                            )
                            Icon(Icons.Sharp.MoreVert, null, modifier = Modifier.size(30.dp))
                        }
                    )
                }
            }
            ListSettingsRow(
                displayType = displayType,
                listType = listType,
                onDisplayTypeChange = { viewModel.setDisplayType(it) },
                onListTypeChange = { viewModel.setListType(it) },
                excludeListTypes = persistentListOf(ListType.ARTISTS, ListType.PLAYLISTS),
            )
        }

        when (listType) {
            ListType.ALBUMS -> {
                val selectedAlbumIds by viewModel.filteredSelectedAlbumIds.collectAsStateWithLifecycle()
                val uiStates by viewModel.albumUiStates.collectAsStateWithLifecycle()
                val albumSelectionCallbacks = viewModel.getAlbumSelectionCallbacks(appCallbacks, context)

                when (displayType) {
                    DisplayType.LIST -> AlbumList(
                        states = { uiStates },
                        selectionCallbacks = albumSelectionCallbacks,
                        selectedAlbumIds = selectedAlbumIds,
                        showArtist = false,
                        onEmpty = {
                            Text(stringResource(R.string.no_albums_found), modifier = Modifier.padding(10.dp))
                        },
                        modifier = Modifier.nestedScroll(nestedScrollConnection),
                        callbacks = albumCallbacks,
                    )
                    DisplayType.GRID -> AlbumGrid(
                        states = { uiStates },
                        callbacks = albumCallbacks,
                        selectionCallbacks = albumSelectionCallbacks,
                        selectedAlbumIds = selectedAlbumIds,
                        modifier = Modifier.nestedScroll(nestedScrollConnection),
                        showArtist = false,
                    ) {
                        Text(stringResource(R.string.no_albums_found), modifier = Modifier.padding(10.dp))
                    }
                }
            }
            ListType.TRACKS -> {
                val latestSelectedTrackId by viewModel.latestSelectedTrackId.collectAsStateWithLifecycle()
                val selectedTrackIds by viewModel.selectedTrackIds.collectAsStateWithLifecycle()
                val trackUiStates: LazyPagingItems<TrackUiState> = viewModel.trackUiStates.collectAsLazyPagingItems()

                var latestSelectedTrackIndex by rememberSaveable(selectedTrackIds) { mutableStateOf<Int?>(null) }

                val trackCallbacks2 = { index: Int, state: TrackUiState ->
                    trackCallbacks.copy(
                        onTrackClick = {
                            if (selectedTrackIds.isNotEmpty()) viewModel.toggleTrackSelected(state.trackId)
                            else if (state.isPlayable) viewModel.playTrack(state)
                        },
                        onLongClick = {
                            viewModel.selectTracksBetweenIndices(
                                fromIndex = latestSelectedTrackIndex,
                                toIndex = index,
                                getTrackIdAtIndex = { trackUiStates[it]?.trackId },
                            )
                        },
                        onEach = {
                            if (state.trackId == latestSelectedTrackId) latestSelectedTrackIndex = index
                        },
                    )
                }

                val trackSelectionCallbacks = viewModel.getTrackSelectionCallbacks(appCallbacks)

                when (displayType) {
                    DisplayType.LIST -> TrackList(
                        uiStates = trackUiStates,
                        trackCallbacks = trackCallbacks2,
                        selectedTrackIds = selectedTrackIds.toImmutableList(),
                        trackSelectionCallbacks = trackSelectionCallbacks,
                        modifier = Modifier.nestedScroll(nestedScrollConnection),
                        showArtist = false,
                        showAlbum = true,
                        ensureTrackMetadata = { viewModel.ensureTrackMetadata(it) },
                    ) {
                        Text(stringResource(R.string.no_tracks_found), modifier = Modifier.padding(10.dp))
                    }
                    DisplayType.GRID -> TrackGrid(
                        uiStates = trackUiStates,
                        showArtist = false,
                        showAlbum = true,
                        trackSelectionCallbacks = trackSelectionCallbacks,
                        selectedTrackIds = selectedTrackIds.toImmutableList(),
                        onEmpty = {
                            Text(stringResource(R.string.no_tracks_found), modifier = Modifier.padding(10.dp))
                        },
                        modifier = Modifier.nestedScroll(nestedScrollConnection),
                        ensureTrackMetadata = { viewModel.ensureTrackMetadata(it) },
                        trackCallbacks = trackCallbacks2,
                    )
                }
            }
            ListType.ARTISTS -> {}
            ListType.PLAYLISTS -> {}
        }
    }
}
