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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListSettingsRow
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.compose.PlaylistList
import us.huseli.thoucylinder.compose.utils.CollapsibleToolbar
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.LibraryViewModel

@Composable
fun LibraryScreenPlaylistTab(
    appCallbacks: AppCallbacks,
    showToolbars: Boolean,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
    listModifier: Modifier = Modifier,
) {
    CollapsibleToolbar(show = showToolbars) {
        ListSettingsRow(
            displayType = DisplayType.LIST,
            listType = ListType.PLAYLISTS,
            onDisplayTypeChange = { viewModel.setDisplayType(it) },
            onListTypeChange = { viewModel.setListType(it) },
            availableDisplayTypes = listOf(DisplayType.LIST),
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        val playlists by viewModel.playlists.collectAsStateWithLifecycle(persistentListOf())
        val isLoadingPlaylists by viewModel.isLoadingPlaylists.collectAsStateWithLifecycle()

        PlaylistList(
            modifier = listModifier,
            playlists = playlists,
            onPlaylistClick = { appCallbacks.onPlaylistClick(it.playlistId) },
            onPlaylistPlayClick = { viewModel.playPlaylist(it.playlistId) },
            isLoading = isLoadingPlaylists,
        )

        FloatingActionButton(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 10.dp),
            onClick = appCallbacks.onCreatePlaylistClick,
            shape = CircleShape,
            content = { Icon(Icons.Sharp.Add, stringResource(R.string.add_playlist)) },
        )
    }
}
