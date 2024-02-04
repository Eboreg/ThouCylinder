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
import us.huseli.thoucylinder.dataclasses.pojos.AlbumPojo
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
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
                    val albumPojos by viewModel.albumPojos.collectAsStateWithLifecycle()
                    val selectedAlbums by viewModel.selectedAlbums.collectAsStateWithLifecycle(emptyList())

                    val albumCallbacks = { pojo: AlbumPojo ->
                        AlbumCallbacks(
                            pojo = pojo,
                            appCallbacks = appCallbacks,
                            context = context,
                            onPlayClick = { viewModel.playAlbum(pojo.album) },
                            onEnqueueClick = { viewModel.enqueueAlbum(pojo.album, context) },
                            onAlbumLongClick = {
                                viewModel.selectAlbumsFromLastSelected(pojo.album, albumPojos.map { it.album })
                            },
                            onAlbumClick = {
                                if (selectedAlbums.isNotEmpty()) viewModel.toggleSelected(pojo.album)
                                else appCallbacks.onAlbumClick(pojo.album.albumId)
                            },
                        )
                    }
                    val albumSelectionCallbacks = AlbumSelectionCallbacks(
                        albums = selectedAlbums,
                        appCallbacks = appCallbacks,
                        onPlayClick = { viewModel.playAlbums(selectedAlbums) },
                        onEnqueueClick = { viewModel.enqueueAlbums(selectedAlbums, context) },
                        onUnselectAllClick = { viewModel.unselectAllAlbums() },
                        onSelectAllClick = { viewModel.selectAlbums(albumPojos.map { it.album }) },
                    )

                    when (displayType) {
                        DisplayType.LIST -> AlbumList(
                            pojos = albumPojos,
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
                            pojos = albumPojos,
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
                    val latestSelectedTrackPojo by viewModel.latestSelectedTrackPojo.collectAsStateWithLifecycle(null)
                    val selectedTrackPojos by viewModel.selectedTrackPojos.collectAsStateWithLifecycle(emptyList())
                    val trackPojos: LazyPagingItems<TrackPojo> = viewModel.trackPojos.collectAsLazyPagingItems()

                    var latestSelectedTrackIndex by rememberSaveable(selectedTrackPojos) { mutableStateOf<Int?>(null) }

                    val trackCallbacks = { index: Int, pojo: TrackPojo ->
                        TrackCallbacks(
                            pojo = pojo,
                            appCallbacks = appCallbacks,
                            context = context,
                            onTrackClick = {
                                if (selectedTrackPojos.isNotEmpty()) viewModel.toggleSelected(pojo)
                                else viewModel.playTrackPojo(pojo)
                            },
                            onEnqueueClick = { viewModel.enqueueTrackPojo(pojo, context) },
                            onLongClick = {
                                viewModel.selectTrackPojos(
                                    latestSelectedTrackIndex?.let { index2 ->
                                        (min(index, index2)..max(index, index2)).mapNotNull { trackPojos[it] }
                                    } ?: listOf(pojo)
                                )
                            },
                            onEach = {
                                if (pojo.track.trackId == latestSelectedTrackPojo?.track?.trackId)
                                    latestSelectedTrackIndex = index
                            },
                        )
                    }
                    val trackSelectionCallbacks = TrackSelectionCallbacks(
                        onAddToPlaylistClick = {
                            appCallbacks.onAddToPlaylistClick(Selection(tracks = selectedTrackPojos.tracks()))
                        },
                        onPlayClick = { viewModel.playTrackPojos(selectedTrackPojos) },
                        onEnqueueClick = { viewModel.enqueueTrackPojos(selectedTrackPojos, context) },
                        onUnselectAllClick = { viewModel.unselectAllTrackPojos() },
                    )
                    val trackDownloadTasks by viewModel.trackDownloadTasks.collectAsStateWithLifecycle(emptyList())

                    when (displayType) {
                        DisplayType.LIST -> TrackList(
                            trackPojos = trackPojos,
                            viewModel = viewModel,
                            showArtist = false,
                            selectedTrackPojos = selectedTrackPojos,
                            trackCallbacks = trackCallbacks,
                            trackDownloadTasks = trackDownloadTasks,
                            trackSelectionCallbacks = trackSelectionCallbacks,
                            onEmpty = {
                                Text(stringResource(R.string.no_tracks_found), modifier = Modifier.padding(10.dp))
                            },
                        )
                        DisplayType.GRID -> TrackGrid(
                            trackPojos = trackPojos,
                            viewModel = viewModel,
                            showArtist = false,
                            trackCallbacks = trackCallbacks,
                            trackSelectionCallbacks = trackSelectionCallbacks,
                            selectedTrackPojos = selectedTrackPojos,
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
