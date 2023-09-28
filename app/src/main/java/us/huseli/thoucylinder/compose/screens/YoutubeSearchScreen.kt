package us.huseli.thoucylinder.compose.screens

import androidx.annotation.MainThread
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.AlbumArt
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.DisplayTypeSelection
import us.huseli.thoucylinder.compose.ObnoxiousProgressIndicator
import us.huseli.thoucylinder.compose.OutlinedTextFieldLabel
import us.huseli.thoucylinder.compose.VideoSection
import us.huseli.thoucylinder.dataclasses.YoutubePlaylist
import us.huseli.thoucylinder.dataclasses.YoutubeVideo
import us.huseli.thoucylinder.viewmodels.YoutubeSearchViewModel
import java.util.UUID

@Composable
fun YoutubeSearchScreen(
    modifier: Modifier = Modifier,
    viewModel: YoutubeSearchViewModel = hiltViewModel(),
    @MainThread onGotoAlbum: (UUID) -> Unit,
) {
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()

    var displayType by rememberSaveable { mutableStateOf(DisplayType.LIST) }

    val onPlaylistClick: (YoutubePlaylist) -> Unit = { playlist ->
        viewModel.addYoutubePlaylistAsTempAlbum(playlist) { onGotoAlbum(it.albumId) }
    }

    Column(modifier = modifier) {
        YoutubeSearchForm(
            isSearching = isSearching,
            onSearch = { viewModel.search(it) },
            modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp),
        )

        if (isSearching) {
            ObnoxiousProgressIndicator(modifier = Modifier.padding(10.dp), tonalElevation = 5.dp)
        }
        if (playlists.isNotEmpty() && !isSearching) {
            DisplayTypeSelection(
                displayType = displayType,
                onDisplayTypeChange = { displayType = it },
            )
        }

        if (!isSearching) {
            when (displayType) {
                DisplayType.LIST -> YoutubeSearchResultsList(
                    playlists = playlists,
                    videos = videos,
                    viewModel = viewModel,
                    onPlaylistClick = onPlaylistClick,
                )
                DisplayType.GRID -> YoutubeSearchResultsGrid(
                    playlists = playlists,
                    videos = videos,
                    viewModel = viewModel,
                    onPlaylistClick = onPlaylistClick,
                )
            }
        }
    }
}


@Composable
fun YoutubeSearchForm(
    modifier: Modifier = Modifier,
    isSearching: Boolean,
    onSearch: (String) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var query by rememberSaveable { mutableStateOf("roxy music for your pleasure") }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            enabled = !isSearching,
            onValueChange = { query = it },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            label = { OutlinedTextFieldLabel(text = stringResource(R.string.search_query)) },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearch(query)
                    keyboardController?.hide()
                },
            ),
            trailingIcon = {
                IconButton(
                    onClick = { onSearch(query) },
                    enabled = !isSearching,
                    content = {
                        Icon(
                            imageVector = Icons.Sharp.Search,
                            contentDescription = stringResource(R.string.search),
                        )
                    }
                )
            },
        )
    }
}


@Composable
fun YoutubeSearchResultsList(
    playlists: List<YoutubePlaylist>,
    videos: List<YoutubeVideo>,
    viewModel: YoutubeSearchViewModel = hiltViewModel(),
    onPlaylistClick: (YoutubePlaylist) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(playlists) { playlist ->
            val thumbnail by viewModel.getPlaylistThumbnail(playlist).collectAsStateWithLifecycle()

            OutlinedCard(
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.fillMaxWidth().height(80.dp).clickable { onPlaylistClick(playlist) },
            ) {
                Row {
                    AlbumArt(image = thumbnail, modifier = Modifier.fillMaxHeight())
                    Text(
                        text = playlist.toString(),
                        modifier = Modifier.padding(5.dp),
                    )
                }
            }
        }
        items(videos) { video ->
            val metadata by viewModel.getVideoMetadata(video).collectAsStateWithLifecycle()

            OutlinedCard(shape = ShapeDefaults.ExtraSmall) {
                VideoSection(video = video, metadata = metadata)
            }
        }
    }
}


@Composable
fun YoutubeSearchResultsGrid(
    playlists: List<YoutubePlaylist>,
    videos: List<YoutubeVideo>,
    viewModel: YoutubeSearchViewModel = hiltViewModel(),
    onPlaylistClick: (YoutubePlaylist) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(playlists) { playlist ->
            val thumbnail by viewModel.getPlaylistThumbnail(playlist).collectAsStateWithLifecycle()

            OutlinedCard(
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.fillMaxHeight().clickable { onPlaylistClick(playlist) },
            ) {
                AlbumArt(image = thumbnail, modifier = Modifier.fillMaxWidth())
                Text(
                    text = playlist.toString(),
                    modifier = Modifier.padding(5.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        items(videos, span = { GridItemSpan(maxLineSpan) }) { video ->
            val metadata by viewModel.getVideoMetadata(video).collectAsStateWithLifecycle()

            OutlinedCard(shape = ShapeDefaults.ExtraSmall) {
                VideoSection(video = video, metadata = metadata)
            }
        }
    }
}
