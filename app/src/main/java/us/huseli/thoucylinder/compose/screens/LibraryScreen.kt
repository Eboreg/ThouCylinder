package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.compose.AlbumArt
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.DisplayTypeSelection
import us.huseli.thoucylinder.dataclasses.Album
import us.huseli.thoucylinder.viewmodels.LibraryViewModel

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
    onAlbumClick: (Album) -> Unit,
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle(initialValue = emptyList())
    val singleTracks by viewModel.singleTracks.collectAsStateWithLifecycle(initialValue = emptyList())
    var displayType by rememberSaveable { mutableStateOf(DisplayType.LIST) }

    Column(modifier = modifier) {
        DisplayTypeSelection(
            displayType = displayType,
            onDisplayTypeChange = { displayType = it },
        )

        when (displayType) {
            DisplayType.LIST -> LibraryList(albums = albums, onAlbumClick = onAlbumClick, viewModel = viewModel)
            DisplayType.GRID -> LibraryGrid(albums = albums, onAlbumClick = onAlbumClick, viewModel = viewModel)
        }
    }
}

@Composable
fun LibraryList(
    albums: List<Album>,
    viewModel: LibraryViewModel,
    onAlbumClick: (Album) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(albums) { album ->
            val albumArt by viewModel.getAlbumArt(album).collectAsStateWithLifecycle()

            OutlinedCard(
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.fillMaxWidth().height(80.dp).clickable { onAlbumClick(album) },
            ) {
                Row {
                    AlbumArt(image = albumArt, modifier = Modifier.fillMaxHeight())
                    Text(
                        text = album.toString(),
                        modifier = Modifier.padding(5.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryGrid(
    albums: List<Album>,
    viewModel: LibraryViewModel,
    onAlbumClick: (Album) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(albums) { album ->
            val albumArt by viewModel.getAlbumArt(album).collectAsStateWithLifecycle()

            OutlinedCard(
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.fillMaxHeight().clickable { onAlbumClick(album) },
            ) {
                AlbumArt(image = albumArt, modifier = Modifier.fillMaxWidth())
                Text(
                    text = album.toString(),
                    modifier = Modifier.padding(5.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
