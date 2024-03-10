package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Radio
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.RadioState
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListSettingsRow
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.compose.screens.library.LibraryScreenAlbumTab
import us.huseli.thoucylinder.compose.screens.library.LibraryScreenArtistTab
import us.huseli.thoucylinder.compose.screens.library.LibraryScreenPlaylistTab
import us.huseli.thoucylinder.compose.screens.library.LibraryScreenTrackTab
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.combos.TrackCombo
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.LibraryViewModel

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
    trackListState: LazyListState = rememberLazyListState(),
    trackGridState: LazyGridState = rememberLazyGridState(),
    appCallbacks: AppCallbacks,
) {
    val albumCombos by viewModel.albumCombos.collectAsStateWithLifecycle(emptyList())
    val displayType by viewModel.displayType.collectAsStateWithLifecycle()
    val listType by viewModel.listType.collectAsStateWithLifecycle()
    val isImportingLocalMedia by viewModel.isImportingLocalMedia.collectAsStateWithLifecycle()
    val radioState by viewModel.radioState.collectAsStateWithLifecycle()
    val trackCombos: LazyPagingItems<TrackCombo> = viewModel.pagingTrackCombos.collectAsLazyPagingItems()

    val availableDisplayTypes =
        if (listType == ListType.PLAYLISTS) listOf(DisplayType.LIST)
        else listOf(DisplayType.LIST, DisplayType.GRID)

    Column(modifier = modifier) {
        ListSettingsRow(
            displayType = displayType,
            listType = listType,
            onDisplayTypeChange = { viewModel.setDisplayType(it) },
            onListTypeChange = { viewModel.setListType(it) },
            availableDisplayTypes = availableDisplayTypes,
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when (listType) {
                ListType.ALBUMS -> LibraryScreenAlbumTab(
                    viewModel = viewModel,
                    appCallbacks = appCallbacks,
                    albumCombos = albumCombos,
                    isImporting = isImportingLocalMedia,
                    displayType = displayType,
                )
                ListType.TRACKS -> LibraryScreenTrackTab(
                    trackCombos = trackCombos,
                    isImporting = isImportingLocalMedia,
                    viewModel = viewModel,
                    appCallbacks = appCallbacks,
                    displayType = displayType,
                    gridState = trackGridState,
                    listState = trackListState,
                )
                ListType.ARTISTS -> LibraryScreenArtistTab(
                    displayType = displayType,
                    isImporting = isImportingLocalMedia,
                    onArtistClick = appCallbacks.onArtistClick,
                )
                ListType.PLAYLISTS -> LibraryScreenPlaylistTab(
                    viewModel = viewModel,
                    appCallbacks = appCallbacks,
                )
            }

            if (listType != ListType.PLAYLISTS) {
                FloatingActionButton(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 10.dp),
                    onClick = {
                        if (radioState == RadioState.INACTIVE) viewModel.startLibraryRadio()
                        else viewModel.deactivateRadio()
                    },
                    shape = CircleShape,
                    content = {
                        if (radioState == RadioState.LOADING) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        else Icon(
                            imageVector = Icons.Sharp.Radio,
                            contentDescription = stringResource(
                                if (radioState == RadioState.INACTIVE) R.string.start_radio
                                else R.string.stop_radio
                            ),
                        )
                    },
                    containerColor = if (radioState == RadioState.INACTIVE)
                        MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.primaryContainer,
                )
            }
        }
    }
}
