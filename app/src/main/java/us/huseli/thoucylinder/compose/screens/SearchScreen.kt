package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.InputChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.SearchType
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.compose.AlbumGrid
import us.huseli.thoucylinder.compose.AlbumList
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListDisplayTypeButton
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.compose.ListTypeChips
import us.huseli.thoucylinder.compose.SearchForm
import us.huseli.thoucylinder.compose.TrackGrid
import us.huseli.thoucylinder.compose.TrackList
import us.huseli.thoucylinder.compose.utils.ObnoxiousProgressIndicator
import us.huseli.thoucylinder.dataclasses.AlbumPojo
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.viewmodels.SearchViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    onAlbumClick: (UUID) -> Unit,
    onAddToPlaylistClick: (Selection) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle(false)
    val youtubeAlbums by viewModel.youtubeAlbums.collectAsStateWithLifecycle(emptyList())
    val youtubeTracks = viewModel.youtubeTracksPaging.collectAsLazyPagingItems()
    val localAlbums by viewModel.localAlbums.collectAsStateWithLifecycle(emptyList())
    val localTracks = viewModel.localTracks.collectAsLazyPagingItems()

    var displaySearchType by rememberSaveable { mutableStateOf(SearchType.YOUTUBE) }
    var displayType by rememberSaveable { mutableStateOf(DisplayType.LIST) }
    var listType by rememberSaveable { mutableStateOf(ListType.ALBUMS) }

    val onAlbumPojoClick = { pojo: AlbumPojo ->
        viewModel.populateTempAlbum(pojo)
        onAlbumClick(pojo.album.albumId)
    }

    Column(modifier = modifier) {
        SearchForm(
            modifier = Modifier.padding(horizontal = 10.dp),
            isSearching = isSearching,
            onSearch = { query -> viewModel.search(query) }
        )

        if (isSearching) {
            ObnoxiousProgressIndicator(modifier = Modifier.padding(10.dp), tonalElevation = 5.dp)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp).weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    InputChip(
                        selected = displaySearchType == SearchType.LOCAL,
                        onClick = { displaySearchType = SearchType.LOCAL },
                        label = { Text(text = stringResource(R.string.local)) },
                    )
                    InputChip(
                        selected = displaySearchType == SearchType.YOUTUBE,
                        onClick = { displaySearchType = SearchType.YOUTUBE },
                        label = { Text(text = stringResource(R.string.youtube)) },
                    )
                    ListTypeChips(
                        current = listType,
                        onChange = { listType = it },
                        exclude = listOf(ListType.ARTISTS, ListType.PLAYLISTS),
                    )
                }
                ListDisplayTypeButton(current = displayType, onChange = { displayType = it })
            }

            when (displaySearchType) {
                SearchType.LOCAL -> {
                    SearchResults(
                        modifier = Modifier.padding(horizontal = 10.dp).fillMaxWidth(),
                        albums = localAlbums,
                        tracks = localTracks,
                        viewModel = viewModel,
                        displayType = displayType,
                        listType = listType,
                        onAlbumClick = onAlbumPojoClick,
                        onAddToPlaylistClick = onAddToPlaylistClick,
                    )
                }
                SearchType.YOUTUBE -> {
                    SearchResults(
                        modifier = Modifier.padding(horizontal = 10.dp).fillMaxWidth(),
                        albums = youtubeAlbums,
                        tracks = youtubeTracks,
                        viewModel = viewModel,
                        displayType = displayType,
                        listType = listType,
                        onAlbumClick = onAlbumPojoClick,
                        onAddToPlaylistClick = onAddToPlaylistClick,
                    )
                }
            }
        }
    }
}


@Composable
fun SearchResults(
    albums: List<AlbumPojo>,
    tracks: LazyPagingItems<Track>,
    viewModel: SearchViewModel,
    displayType: DisplayType,
    listType: ListType,
    onAlbumClick: (AlbumPojo) -> Unit,
    onAddToPlaylistClick: (Selection) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (displayType) {
        DisplayType.LIST -> when (listType) {
            ListType.ALBUMS -> {
                AlbumList(
                    modifier = modifier,
                    albums = albums,
                    viewModel = viewModel,
                    onAlbumClick = onAlbumClick,
                )
            }
            ListType.TRACKS -> {
                TrackList(
                    modifier = modifier,
                    tracks = tracks,
                    viewModel = viewModel,
                    onDownloadClick = { viewModel.downloadTrack(it) },
                    onPlayOrPauseClick = { viewModel.playOrPause(it) },
                    onAddToPlaylistClick = onAddToPlaylistClick,
                )
            }
            ListType.ARTISTS -> {}
            ListType.PLAYLISTS -> {}
        }
        DisplayType.GRID -> when (listType) {
            ListType.ALBUMS -> {
                AlbumGrid(
                    modifier = modifier,
                    albums = albums,
                    viewModel = viewModel,
                    onAlbumClick = onAlbumClick,
                )
            }
            ListType.TRACKS -> {
                TrackGrid(
                    modifier = modifier,
                    tracks = tracks,
                    viewModel = viewModel,
                    onAddToPlaylistClick = onAddToPlaylistClick,
                )
            }
            ListType.ARTISTS -> {}
            ListType.PLAYLISTS -> {}
        }
    }
}
