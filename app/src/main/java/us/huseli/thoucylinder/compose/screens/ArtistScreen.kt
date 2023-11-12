package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.compose.AlbumGrid
import us.huseli.thoucylinder.compose.AlbumList
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListSettingsRow
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.compose.TrackGrid
import us.huseli.thoucylinder.compose.TrackList
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import us.huseli.thoucylinder.viewmodels.ArtistViewModel

@Composable
fun ArtistScreen(
    modifier: Modifier = Modifier,
    viewModel: ArtistViewModel = hiltViewModel(),
    appCallbacks: AppCallbacks,
) {
    val artist = viewModel.artist
    val displayType by viewModel.displayType.collectAsStateWithLifecycle()
    val listType by viewModel.listType.collectAsStateWithLifecycle()
    val tracks: LazyPagingItems<TrackPojo> = viewModel.tracks.collectAsLazyPagingItems()
    val albumPojos by viewModel.albumPojos.collectAsStateWithLifecycle()
    val selectedTracks by viewModel.selectedTracks.collectAsStateWithLifecycle(emptyList())
    val selectedAlbums by viewModel.selectedAlbums.collectAsStateWithLifecycle(emptyList())
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
                    content = { Icon(Icons.AutoMirrored.Sharp.ArrowBack, stringResource(R.string.go_back)) }
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
                    val albumCallbacks = { pojo: AbstractAlbumPojo ->
                        AlbumCallbacks.fromAppCallbacks(
                            album = pojo.album,
                            appCallbacks = appCallbacks,
                            onPlayClick = { viewModel.playAlbum(pojo.album) },
                            onEnqueueClick = { viewModel.enqueueAlbum(pojo.album, context) },
                            onRemoveFromLibraryClick = { viewModel.removeAlbumFromLibrary(pojo.album) },
                            onAlbumLongClick = { viewModel.selectAlbumsFromLastSelected(pojo.album) },
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
                        )
                    }
                }
                ListType.TRACKS -> {
                    val trackCallbacks = { pojo: TrackPojo ->
                        TrackCallbacks.fromAppCallbacks(
                            pojo = pojo,
                            appCallbacks = appCallbacks,
                            onTrackClick = {
                                if (selectedTracks.isNotEmpty()) viewModel.toggleSelected(pojo)
                                else viewModel.playTrackPojo(pojo)
                            },
                            onEnqueueClick = { viewModel.enqueueTrackPojo(pojo, context) },
                            onLongClick = { viewModel.selectTracksFromLastSelected(to = pojo) },
                        )
                    }
                    val trackSelectionCallbacks = TrackSelectionCallbacks(
                        onAddToPlaylistClick = { appCallbacks.onAddToPlaylistClick(Selection(trackPojos = selectedTracks)) },
                        onPlayClick = { viewModel.playTrackPojos(selectedTracks) },
                        onEnqueueClick = { viewModel.enqueueTrackPojos(selectedTracks, context) },
                        onUnselectAllClick = { viewModel.unselectAllTracks() },
                    )

                    when (displayType) {
                        DisplayType.LIST -> TrackList(
                            trackPojos = tracks,
                            viewModel = viewModel,
                            showArtist = false,
                            selectedTracks = selectedTracks,
                            trackCallbacks = trackCallbacks,
                            trackSelectionCallbacks = trackSelectionCallbacks,
                            onEmpty = {
                                Text(stringResource(R.string.no_tracks_found), modifier = Modifier.padding(10.dp))
                            },
                        )
                        DisplayType.GRID -> TrackGrid(
                            trackPojos = tracks,
                            viewModel = viewModel,
                            showArtist = false,
                            trackCallbacks = trackCallbacks,
                            trackSelectionCallbacks = trackSelectionCallbacks,
                            selectedTracks = selectedTracks,
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
