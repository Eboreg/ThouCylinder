package us.huseli.thoucylinder.compose.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.PlaylistList
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.LibraryViewModel

@Composable
fun LibraryScreenPlaylistTab(
    viewModel: LibraryViewModel,
    appCallbacks: AppCallbacks,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        val playlists by viewModel.playlists.collectAsStateWithLifecycle(emptyList())
        val isLoadingPlaylists by viewModel.isLoadingPlaylists.collectAsStateWithLifecycle()

        PlaylistList(
            playlists = playlists,
            onPlaylistClick = { appCallbacks.onPlaylistClick(it.playlistId) },
            onPlaylistPlayClick = { viewModel.playPlaylist(it.playlistId) },
            isLoading = isLoadingPlaylists,
            getImage = { viewModel.getPlaylistImage(it, context) },
        )

        FloatingActionButton(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 10.dp),
            onClick = appCallbacks.onCreatePlaylistClick,
            shape = CircleShape,
            content = { Icon(Icons.Sharp.Add, stringResource(R.string.add_playlist)) },
        )
    }
}
