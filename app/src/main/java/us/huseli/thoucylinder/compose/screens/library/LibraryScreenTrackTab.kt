package us.huseli.thoucylinder.compose.screens.library

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.TrackSortParameter
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.track.TrackGrid
import us.huseli.thoucylinder.compose.track.TrackList
import us.huseli.thoucylinder.compose.utils.ListActions
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.abstr.tracks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.combos.TrackCombo
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.LibraryViewModel
import kotlin.math.max
import kotlin.math.min

@Composable
fun LibraryScreenTrackTab(
    trackCombos: LazyPagingItems<TrackCombo>,
    isImporting: Boolean,
    viewModel: LibraryViewModel,
    appCallbacks: AppCallbacks,
    displayType: DisplayType,
    gridState: LazyGridState,
    listState: LazyListState,
) {
    val context = LocalContext.current
    val isLoadingTracks by viewModel.isLoadingTracks.collectAsStateWithLifecycle()
    val latestSelectedTrackCombo by viewModel.latestSelectedTrackCombo.collectAsStateWithLifecycle(null)
    val searchTerm by viewModel.trackSearchTerm.collectAsStateWithLifecycle()
    val selectedTrackCombos by viewModel.selectedTrackCombos.collectAsStateWithLifecycle()
    val sortOrder by viewModel.trackSortOrder.collectAsStateWithLifecycle()
    val sortParameter by viewModel.trackSortParameter.collectAsStateWithLifecycle()
    val trackDownloadTasks by viewModel.trackDownloadTasks.collectAsStateWithLifecycle(emptyList())

    var latestSelectedIndex by rememberSaveable(selectedTrackCombos) { mutableStateOf<Int?>(null) }

    val trackCallbacks = { index: Int, combo: TrackCombo ->
        TrackCallbacks(
            combo = combo,
            appCallbacks = appCallbacks,
            context = context,
            onTrackClick = {
                if (selectedTrackCombos.isNotEmpty()) viewModel.toggleSelected(combo)
                else viewModel.playTrackCombo(combo)
            },
            onEnqueueClick = { viewModel.enqueueTrackCombo(combo, context) },
            onLongClick = {
                viewModel.selectTrackCombos(
                    latestSelectedIndex?.let { index2 ->
                        (min(index, index2)..max(index, index2)).mapNotNull { idx -> trackCombos[idx] }
                    } ?: listOf(combo)
                )
            },
            onEach = {
                if (combo.track.trackId == latestSelectedTrackCombo?.track?.trackId)
                    latestSelectedIndex = index
            },
        )
    }
    val trackSelectionCallbacks = TrackSelectionCallbacks(
        onAddToPlaylistClick = {
            appCallbacks.onAddToPlaylistClick(Selection(tracks = selectedTrackCombos.tracks()))
        },
        onPlayClick = { viewModel.playTrackCombos(selectedTrackCombos) },
        onEnqueueClick = { viewModel.enqueueTrackCombos(selectedTrackCombos, context) },
        onUnselectAllClick = { viewModel.unselectAllTrackCombos() },
    )
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

    ListActions(
        initialSearchTerm = searchTerm,
        sortParameter = sortParameter,
        sortOrder = sortOrder,
        sortParameters = TrackSortParameter.withLabels(context),
        sortDialogTitle = stringResource(R.string.track_order),
        onSort = { param, order -> viewModel.setTrackSorting(param, order) },
        onSearch = { viewModel.setTrackSearchTerm(it) },
    )

    when (displayType) {
        DisplayType.LIST -> TrackList(
            trackCombos = trackCombos,
            selectedTrackCombos = selectedTrackCombos,
            viewModel = viewModel,
            listState = listState,
            trackCallbacks = trackCallbacks,
            trackSelectionCallbacks = trackSelectionCallbacks,
            trackDownloadTasks = trackDownloadTasks,
            onEmpty = onEmpty,
            progressIndicatorText = progressIndicatorText,
        )
        DisplayType.GRID -> TrackGrid(
            trackCombos = trackCombos,
            viewModel = viewModel,
            gridState = gridState,
            trackCallbacks = trackCallbacks,
            trackSelectionCallbacks = trackSelectionCallbacks,
            selectedTrackCombos = selectedTrackCombos,
            trackDownloadTasks = trackDownloadTasks,
            onEmpty = onEmpty,
            progressIndicatorText = progressIndicatorText,
        )
    }
}
