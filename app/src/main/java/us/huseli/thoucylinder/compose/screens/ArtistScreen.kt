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
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.ArtistContextMenu
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListSettingsRow
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.compose.album.AlbumGrid
import us.huseli.thoucylinder.compose.album.AlbumList
import us.huseli.thoucylinder.compose.track.TrackGrid
import us.huseli.thoucylinder.compose.track.TrackList
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.combos.AlbumCombo
import us.huseli.thoucylinder.dataclasses.combos.TrackCombo
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.ArtistViewModel

@Composable
fun ArtistScreen(
    modifier: Modifier = Modifier,
    viewModel: ArtistViewModel = hiltViewModel(),
    appCallbacks: AppCallbacks,
) {
    val uriHandler = LocalUriHandler.current
    val artist by viewModel.artist.collectAsStateWithLifecycle(null)
    val displayType by viewModel.displayType.collectAsStateWithLifecycle()
    val listType by viewModel.listType.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var isContextMenuShown by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            color = BottomAppBarDefaults.containerColor,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.padding(horizontal = 10.dp),
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
                                onStartRadioClick = { appCallbacks.onStartArtistRadioClick(artist.id) },
                                onEnqueueClick = { viewModel.enqueueArtist(context) },
                                onAddToPlaylistClick = {
                                    viewModel.onAllArtistTracks {
                                        appCallbacks.onAddToPlaylistClick(Selection(tracks = it))
                                    }
                                },
                                onSpotifyClick = artist.spotifyWebUrl?.let { { uriHandler.openUri(it) } },
                            )
                            Icon(Icons.Sharp.MoreVert, null, modifier = Modifier.size(30.dp))
                        }
                    )
                }
            }
        }

        ListSettingsRow(
            displayType = displayType,
            listType = listType,
            onDisplayTypeChange = { viewModel.setDisplayType(it) },
            onListTypeChange = { viewModel.setListType(it) },
            excludeListTypes = listOf(ListType.ARTISTS, ListType.PLAYLISTS),
        )

        Column {
            when (listType) {
                ListType.ALBUMS -> {
                    val albumDownloadTasks by viewModel.albumDownloadTasks.collectAsStateWithLifecycle()
                    val albumCombos by viewModel.albumCombos.collectAsStateWithLifecycle(emptyList())
                    val selectedAlbumIds by viewModel.filteredSelectedAlbumIds.collectAsStateWithLifecycle(emptyList())

                    val albumCallbacks = { combo: AlbumCombo ->
                        AlbumCallbacks(
                            combo = combo,
                            appCallbacks = appCallbacks,
                            context = context,
                            onPlayClick = if (combo.album.isPlayable) {
                                { viewModel.playAlbum(combo.album.albumId) }
                            } else null,
                            onEnqueueClick = if (combo.album.isPlayable) {
                                { viewModel.enqueueAlbum(combo.album.albumId, context) }
                            } else null,
                            onAlbumLongClick = {
                                viewModel.selectAlbumsFromLastSelected(
                                    combo.album.albumId,
                                    albumCombos.map { it.album.albumId },
                                )
                            },
                            onAlbumClick = {
                                if (selectedAlbumIds.isNotEmpty()) viewModel.toggleAlbumSelected(combo.album.albumId)
                                else appCallbacks.onAlbumClick(combo.album.albumId)
                            },
                        )
                    }
                    val albumSelectionCallbacks = viewModel.getAlbumSelectionCallbacks(appCallbacks, context)

                    when (displayType) {
                        DisplayType.LIST -> AlbumList(
                            combos = albumCombos,
                            albumCallbacks = albumCallbacks,
                            albumSelectionCallbacks = albumSelectionCallbacks,
                            selectedAlbumIds = selectedAlbumIds,
                            showArtist = false,
                            onEmpty = {
                                Text(stringResource(R.string.no_albums_found), modifier = Modifier.padding(10.dp))
                            },
                            albumDownloadTasks = albumDownloadTasks,
                            getThumbnail = { viewModel.getAlbumThumbnail(it, context) },
                        )
                        DisplayType.GRID -> AlbumGrid(
                            combos = albumCombos,
                            albumCallbacks = albumCallbacks,
                            albumSelectionCallbacks = albumSelectionCallbacks,
                            selectedAlbumIds = selectedAlbumIds,
                            showArtist = false,
                            onEmpty = {
                                Text(stringResource(R.string.no_albums_found), modifier = Modifier.padding(10.dp))
                            },
                            albumDownloadTasks = albumDownloadTasks,
                        )
                    }
                }
                ListType.TRACKS -> {
                    val latestSelectedTrackId by viewModel.latestSelectedTrackId.collectAsStateWithLifecycle(null)
                    val selectedTrackIds by viewModel.selectedTrackIds.collectAsStateWithLifecycle(emptyList())
                    val trackCombos: LazyPagingItems<TrackCombo> = viewModel.trackCombos.collectAsLazyPagingItems()

                    var latestSelectedTrackIndex by rememberSaveable(selectedTrackIds) { mutableStateOf<Int?>(null) }

                    val trackCallbacks = { index: Int, combo: TrackCombo ->
                        TrackCallbacks(
                            combo = combo,
                            appCallbacks = appCallbacks,
                            context = context,
                            onTrackClick = {
                                if (selectedTrackIds.isNotEmpty()) viewModel.toggleTrackSelected(combo.track.trackId)
                                else if (combo.track.isPlayable) viewModel.playTrackCombo(combo)
                            },
                            onEnqueueClick = if (combo.track.isPlayable) {
                                { viewModel.enqueueTrackCombo(combo, context) }
                            } else null,
                            onLongClick = {
                                viewModel.selectTracksBetweenIndices(
                                    fromIndex = latestSelectedTrackIndex,
                                    toIndex = index,
                                    getTrackIdAtIndex = { trackCombos[it]?.track?.trackId },
                                )
                            },
                            onEach = {
                                if (combo.track.trackId == latestSelectedTrackId) latestSelectedTrackIndex = index
                            },
                        )
                    }
                    val trackSelectionCallbacks = viewModel.getTrackSelectionCallbacks(appCallbacks, context)
                    val trackDownloadTasks by viewModel.trackDownloadTasks.collectAsStateWithLifecycle(emptyList())

                    when (displayType) {
                        DisplayType.LIST -> TrackList(
                            trackCombos = trackCombos,
                            showAlbum = true,
                            viewModel = viewModel,
                            showArtist = false,
                            selectedTrackIds = selectedTrackIds,
                            trackCallbacks = trackCallbacks,
                            trackDownloadTasks = trackDownloadTasks,
                            trackSelectionCallbacks = trackSelectionCallbacks,
                            onEmpty = {
                                Text(stringResource(R.string.no_tracks_found), modifier = Modifier.padding(10.dp))
                            },
                        )
                        DisplayType.GRID -> TrackGrid(
                            trackCombos = trackCombos,
                            viewModel = viewModel,
                            showArtist = false,
                            showAlbum = true,
                            trackCallbacks = trackCallbacks,
                            trackSelectionCallbacks = trackSelectionCallbacks,
                            selectedTrackIds = selectedTrackIds,
                            trackDownloadTasks = trackDownloadTasks,
                            onEmpty = {
                                Text(stringResource(R.string.no_tracks_found), modifier = Modifier.padding(10.dp))
                            },
                        )
                    }
                }
                ListType.ARTISTS -> {}
                ListType.PLAYLISTS -> {}
            }
        }
    }
}
