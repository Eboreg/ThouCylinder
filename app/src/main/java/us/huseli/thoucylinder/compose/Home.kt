package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun Home(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    listState: LazyListState = rememberLazyListState(),
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle(initialValue = emptyList())
    val singleTracks by viewModel.singleTracks.collectAsStateWithLifecycle(initialValue = emptyList())

    var query by rememberSaveable { mutableStateOf("frank zappa hot rats") }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier,
    ) {
        item {
            Text(
                text = stringResource(R.string.albums),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
        }
        items(albums) { album ->
            Card(modifier = modifier, shape = ShapeDefaults.ExtraSmall) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = album.toString())
                    album.tracks.forEach { track ->
                        Text(text = track.toString(), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.single_tracks),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
        }
        items(singleTracks) { track ->
            Card(modifier = modifier, shape = ShapeDefaults.ExtraSmall) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = track.toString(), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    enabled = !isLoading,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text(text = stringResource(R.string.search_query)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Search,
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            viewModel.search(query)
                            keyboardController?.hide()
                        },
                    ),
                )
                TextButton(
                    onClick = { viewModel.search(query) },
                    enabled = !isLoading,
                    content = { Text(stringResource(R.string.submit)) },
                )
            }
        }

        if (isLoading) {
            item {
                ObnoxiousProgressIndicator(modifier = Modifier.padding(horizontal = 10.dp))
            }
        }

        item {
            Text(
                text = stringResource(R.string.playlists),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
        }
        items(playlists) { playlist ->
            PlaylistSection(playlist = playlist)
        }

        item {
            Text(
                text = stringResource(R.string.videos),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
        }
        items(videos) { video ->
            VideoSection(video = video, modifier = Modifier.padding(horizontal = 10.dp))
        }
    }
}
