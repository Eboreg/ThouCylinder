package us.huseli.thoucylinder.compose.screens.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListSettingsRow
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.compose.track.TrackGrid
import us.huseli.thoucylinder.compose.track.TrackList
import us.huseli.thoucylinder.compose.utils.CollapsibleToolbar
import us.huseli.thoucylinder.compose.utils.ListActions
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.uistates.TrackUiState
import us.huseli.thoucylinder.enums.AvailabilityFilter
import us.huseli.thoucylinder.enums.TrackSortParameter
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.LibraryViewModel

@Composable
fun LibraryScreenTrackTab(
    uiStates: LazyPagingItems<TrackUiState>,
    appCallbacks: AppCallbacks,
    trackCallbacks: TrackCallbacks,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState(),
    isImporting: Boolean = false,
    displayType: DisplayType = DisplayType.LIST,
    showToolbars: () -> Boolean = { true },
    listModifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    val availabilityFilter by viewModel.availabilityFilter.collectAsStateWithLifecycle()
    val isLoadingTracks by viewModel.isLoadingTracks.collectAsStateWithLifecycle()
    val latestSelectedTrackId by viewModel.latestSelectedTrackId.collectAsStateWithLifecycle()
    val searchTerm by viewModel.trackSearchTerm.collectAsStateWithLifecycle()
    val selectedTagPojos by viewModel.selectedTrackTagPojos.collectAsStateWithLifecycle()
    val selectedTrackIds by viewModel.selectedTrackIds.collectAsStateWithLifecycle()
    val sortOrder by viewModel.trackSortOrder.collectAsStateWithLifecycle()
    val sortParameter by viewModel.trackSortParameter.collectAsStateWithLifecycle()
    val tagPojos by viewModel.trackTagPojos.collectAsStateWithLifecycle()

    var latestSelectedIndex by rememberSaveable(selectedTrackIds) { mutableStateOf<Int?>(null) }

    val trackCallbacks3 = remember {
        { index: Int, state: TrackUiState ->
            trackCallbacks.copy(
                onTrackClick = {
                    if (selectedTrackIds.isNotEmpty()) viewModel.toggleTrackSelected(state.trackId)
                    else if (state.isPlayable) viewModel.playTrack(state)
                },
                onLongClick = {
                    if (selectedTrackIds.isEmpty()) viewModel.toggleTrackSelected(state.trackId)
                    else viewModel.selectTracksBetweenIndices(
                        fromIndex = latestSelectedIndex,
                        toIndex = index,
                        getTrackIdAtIndex = { uiStates[it]?.trackId },
                    )
                },
                onEach = {
                    if (state.trackId == latestSelectedTrackId)
                        latestSelectedIndex = index
                },
            )
        }
    }

    val trackSelectionCallbacks = remember { viewModel.getTrackSelectionCallbacks(appCallbacks) }

    val progressIndicatorText =
        if (isImporting) stringResource(R.string.importing_local_tracks)
        else if (isLoadingTracks) stringResource(R.string.loading_tracks)
        else null

    val onEmpty: @Composable () -> Unit = {
        if (!isImporting && !isLoadingTracks) {
            Text(
                stringResource(R.string.no_tracks_found),
                modifier = Modifier.padding(10.dp)
            )
        }
    }

    CollapsibleToolbar(show = showToolbars) {
        ListSettingsRow(
            displayType = displayType,
            listType = ListType.TRACKS,
            onDisplayTypeChange = { viewModel.setDisplayType(it) },
            onListTypeChange = { viewModel.setListType(it) },
            availableDisplayTypes = persistentListOf(DisplayType.LIST, DisplayType.GRID),
        )
        ListActions(
            initialSearchTerm = searchTerm,
            sortParameter = sortParameter,
            sortOrder = sortOrder,
            sortParameters = TrackSortParameter.withLabels(context),
            sortDialogTitle = stringResource(R.string.track_order),
            onSort = { param, order -> viewModel.setTrackSorting(param, order) },
            onSearch = { viewModel.setTrackSearchTerm(it) },
            filterButtonSelected = selectedTagPojos.isNotEmpty() || availabilityFilter != AvailabilityFilter.ALL,
            tagPojos = { tagPojos },
            selectedTagPojos = { selectedTagPojos },
            availabilityFilter = availabilityFilter,
            onTagsChange = { viewModel.setSelectedTrackTagPojos(it) },
            onAvailabilityFilterChange = { viewModel.setAvailabilityFilter(it) },
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        when (displayType) {
            DisplayType.LIST -> TrackList(
                uiStates = uiStates,
                trackCallbacks = trackCallbacks3,
                selectedTrackIds = selectedTrackIds.toImmutableList(),
                trackSelectionCallbacks = trackSelectionCallbacks,
                modifier = listModifier,
                listState = listState,
                progressIndicatorText = progressIndicatorText,
                ensureTrackMetadata = { viewModel.ensureTrackMetadataAsync(it) },
                onEmpty = onEmpty,
            )
            DisplayType.GRID -> TrackGrid(
                uiStates = uiStates,
                ensureTrackMetadata = { viewModel.ensureTrackMetadataAsync(it) },
                gridState = gridState,
                modifier = listModifier,
                onEmpty = onEmpty,
                progressIndicatorText = progressIndicatorText,
                selectedTrackIds = selectedTrackIds.toImmutableList(),
                trackCallbacks = trackCallbacks3,
                trackSelectionCallbacks = trackSelectionCallbacks,
            )
        }
    }
}
