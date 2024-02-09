package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowBack
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListSettingsRow
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.compose.album.AlbumGrid
import us.huseli.thoucylinder.compose.album.AlbumList
import us.huseli.thoucylinder.compose.track.TrackGrid
import us.huseli.thoucylinder.compose.track.TrackList
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.abstr.tracks
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.combos.AlbumCombo
import us.huseli.thoucylinder.dataclasses.combos.TrackCombo
import us.huseli.thoucylinder.viewmodels.ArtistViewModel
import kotlin.math.max
import kotlin.math.min

@Composable
fun ArtistScreen(
    modifier: Modifier = Modifier,
    viewModel: ArtistViewModel = hiltViewModel(),
    appCallbacks: AppCallbacks,
) {
    val artist = viewModel.artist
    val displayType by viewModel.displayType.collectAsStateWithLifecycle()
    val listType by viewModel.listType.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            color = BottomAppBarDefaults.containerColor,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = appCallbacks.onBackClick,
                    content = { Icon(Icons.Sharp.ArrowBack, stringResource(R.string.go_back)) }
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
                    val albumCombos by viewModel.albumCombos.collectAsStateWithLifecycle()
                    val selectedAlbums by viewModel.selectedAlbums.collectAsStateWithLifecycle(emptyList())

                    val albumCallbacks = { combo: AlbumCombo ->
                        AlbumCallbacks(
                            combo = combo,
                            appCallbacks = appCallbacks,
                            context = context,
                            onPlayClick = { viewModel.playAlbum(combo.album) },
                            onEnqueueClick = { viewModel.enqueueAlbum(combo.album, context) },
                            onAlbumLongClick = {
                                viewModel.selectAlbumsFromLastSelected(combo.album, albumCombos.map { it.album })
                            },
                            onAlbumClick = {
                                if (selectedAlbums.isNotEmpty()) viewModel.toggleSelected(combo.album)
                                else appCallbacks.onAlbumClick(combo.album.albumId)
                            },
                        )
                    }
                    val albumSelectionCallbacks = AlbumSelectionCallbacks(
                        albums = selectedAlbums,
                        appCallbacks = appCallbacks,
                        onPlayClick = { viewModel.playAlbums(selectedAlbums) },
                        onEnqueueClick = { viewModel.enqueueAlbums(selectedAlbums, context) },
                        onUnselectAllClick = { viewModel.unselectAllAlbums() },
                        onSelectAllClick = { viewModel.selectAlbums(albumCombos.map { it.album }) },
                    )

                    when (displayType) {
                        DisplayType.LIST -> AlbumList(
                            combos = albumCombos,
                            albumCallbacks = albumCallbacks,
                            albumSelectionCallbacks = albumSelectionCallbacks,
                            selectedAlbums = selectedAlbums,
                            showArtist = false,
                            onEmpty = {
                                Text(stringResource(R.string.no_albums_found), modifier = Modifier.padding(10.dp))
                            },
                            albumDownloadTasks = albumDownloadTasks,
                        )
                        DisplayType.GRID -> AlbumGrid(
                            combos = albumCombos,
                            albumCallbacks = albumCallbacks,
                            albumSelectionCallbacks = albumSelectionCallbacks,
                            selectedAlbums = selectedAlbums,
                            showArtist = false,
                            onEmpty = {
                                Text(stringResource(R.string.no_albums_found), modifier = Modifier.padding(10.dp))
                            },
                            albumDownloadTasks = albumDownloadTasks,
                        )
                    }
                }
                ListType.TRACKS -> {
                    val latestSelectedTrackCombo by viewModel.latestSelectedTrackCombo.collectAsStateWithLifecycle(null)
                    val selectedTrackCombos by viewModel.selectedTrackCombos.collectAsStateWithLifecycle(emptyList())
                    val trackCombos: LazyPagingItems<TrackCombo> = viewModel.trackCombos.collectAsLazyPagingItems()

                    var latestSelectedTrackIndex by rememberSaveable(selectedTrackCombos) { mutableStateOf<Int?>(null) }

                    val trackCallbacks = { index: Int, combo: TrackCombo ->
                        TrackCallbacks(
                            combo = combo,
                            appCallbacks = appCallbacks,
                            context = context,
                            onTrackClick = {
                                if (selectedTrackCombos.isNotEmpty()) viewModel.toggleSelected(combo)
                                else viewModel.playTrackCombo(combo)
                            },
                            onEnqueueClick = { viewModel.enqueueTrackCombo(combo, context) },
                            onLongClick = {
                                viewModel.selectTrackCombos(
                                    latestSelectedTrackIndex?.let { index2 ->
                                        (min(index, index2)..max(index, index2)).mapNotNull { trackCombos[it] }
                                    } ?: listOf(combo)
                                )
                            },
                            onEach = {
                                if (combo.track.trackId == latestSelectedTrackCombo?.track?.trackId)
                                    latestSelectedTrackIndex = index
                            },
                        )
                    }
                    val trackSelectionCallbacks = TrackSelectionCallbacks(
                        onAddToPlaylistClick = {
                            appCallbacks.onAddToPlaylistClick(Selection(tracks = selectedTrackCombos.tracks()))
                        },
                        onPlayClick = { viewModel.playTrackCombos(selectedTrackCombos) },
                        onEnqueueClick = { viewModel.enqueueTrackCombos(selectedTrackCombos, context) },
                        onUnselectAllClick = { viewModel.unselectAllTrackCombos() },
                    )
                    val trackDownloadTasks by viewModel.trackDownloadTasks.collectAsStateWithLifecycle(emptyList())

                    when (displayType) {
                        DisplayType.LIST -> TrackList(
                            trackCombos = trackCombos,
                            viewModel = viewModel,
                            showArtist = false,
                            selectedTrackCombos = selectedTrackCombos,
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
                            trackCallbacks = trackCallbacks,
                            trackSelectionCallbacks = trackSelectionCallbacks,
                            selectedTrackCombos = selectedTrackCombos,
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
