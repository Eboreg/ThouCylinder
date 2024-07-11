package us.huseli.thoucylinder.compose.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.playlist.PlaylistBottomSheetWithButton
import us.huseli.thoucylinder.compose.playlist.PlaylistCollection
import us.huseli.thoucylinder.dataclasses.callbacks.LocalAppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.LocalAppDialogCallbacks
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.PlaylistListViewModel

@Composable
fun LibraryScreenPlaylistTab(
    displayType: DisplayType,
    modifier: Modifier = Modifier,
    viewModel: PlaylistListViewModel = hiltViewModel(),
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val uiStates by viewModel.playlistUiStates.collectAsStateWithLifecycle()
        val isLoadingPlaylists by viewModel.isLoading.collectAsStateWithLifecycle()
        val isEmpty by viewModel.isEmpty.collectAsStateWithLifecycle()
        val appCallbacks = LocalAppCallbacks.current
        val dialogCallbacks = LocalAppDialogCallbacks.current

        PlaylistCollection(
            displayType = displayType,
            uiStates = { uiStates },
            isLoading = isLoadingPlaylists,
            modifier = modifier,
            onEmpty = { if (isEmpty) Text(stringResource(R.string.no_playlists_found)) },
            contextMenu = { state ->
                PlaylistBottomSheetWithButton(
                    name = state.name,
                    thumbnailUris = state.thumbnailUris,
                    onPlayClick = { viewModel.playPlaylist(state.id) },
                    onExportClick = { dialogCallbacks.onExportPlaylistClick(state.id) },
                    onDeleteClick = {
                        viewModel.deletePlaylist(
                            playlistId = state.id,
                            onGotoPlaylistClick = { appCallbacks.onGotoPlaylistClick(state.id) },
                        )
                    },
                    onRename = { viewModel.renamePlaylist(state.id, it) },
                )
            },
        )

        FloatingActionButton(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 10.dp),
            onClick = dialogCallbacks.onCreatePlaylistClick,
            shape = CircleShape,
            content = { Icon(Icons.Sharp.Add, stringResource(R.string.add_playlist)) },
        )
    }
}
