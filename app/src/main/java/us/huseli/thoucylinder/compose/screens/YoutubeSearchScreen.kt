package us.huseli.thoucylinder.compose.screens

import androidx.annotation.MainThread
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import us.huseli.thoucylinder.compose.AlbumGrid
import us.huseli.thoucylinder.compose.AlbumList
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListSettings
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.compose.utils.ObnoxiousProgressIndicator
import us.huseli.thoucylinder.compose.OutlinedTextFieldLabel
import us.huseli.thoucylinder.compose.TrackGrid
import us.huseli.thoucylinder.compose.TrackList
import us.huseli.thoucylinder.dataclasses.Album
import us.huseli.thoucylinder.viewmodels.YoutubeSearchViewModel
import java.util.UUID

@Composable
fun YoutubeSearchScreen(
    modifier: Modifier = Modifier,
    viewModel: YoutubeSearchViewModel = hiltViewModel(),
    @MainThread onGotoAlbum: (UUID) -> Unit,
) {
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()

    var displayType by rememberSaveable { mutableStateOf(DisplayType.LIST) }
    var listType by rememberSaveable { mutableStateOf(ListType.ALBUMS) }

    val onAlbumClick: (Album) -> Unit = { album ->
        viewModel.populateTempAlbum(album)
        onGotoAlbum(album.albumId)
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

        if (albums.isNotEmpty() && !isSearching) {
            ListSettings(
                displayType = displayType,
                listType = listType,
                onDisplayTypeChange = { displayType = it },
                onListTypeChange = { listType = it },
                excludeListTypes = listOf(ListType.ARTISTS),
            )
        }

        if (!isSearching) {
            when (displayType) {
                DisplayType.LIST -> {
                    if (listType == ListType.ALBUMS) {
                        AlbumList(
                            albums = albums,
                            viewModel = viewModel,
                            onAlbumClick = onAlbumClick,
                        )
                    } else if (listType == ListType.TRACKS) {
                        TrackList(
                            tracks = tracks,
                            viewModel = viewModel,
                            onDownloadClick = { viewModel.downloadTrack(it) },
                            onPlayOrPauseClick = { viewModel.playOrPause(it) },
                        )
                    }
                }
                DisplayType.GRID -> {
                    if (listType == ListType.ALBUMS) {
                        AlbumGrid(
                            albums = albums,
                            viewModel = viewModel,
                            onAlbumClick = onAlbumClick,
                        )
                    } else if (listType == ListType.TRACKS) {
                        TrackGrid(
                            tracks = tracks,
                            viewModel = viewModel,
                            onDownloadClick = { viewModel.downloadTrack(it) },
                            onPlayOrPauseClick = { viewModel.playOrPause(it) },
                        )
                    }
                }
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
