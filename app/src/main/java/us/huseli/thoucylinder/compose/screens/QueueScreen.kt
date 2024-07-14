package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.scrollbar.rememberScrollbarListState
import us.huseli.thoucylinder.compose.track.TrackListReorderable
import us.huseli.thoucylinder.compose.utils.BasicHeader
import us.huseli.thoucylinder.compose.utils.BottomSheetItem
import us.huseli.thoucylinder.compose.utils.SelectionAction
import us.huseli.thoucylinder.compose.utils.SmallOutlinedButton
import us.huseli.thoucylinder.dataclasses.callbacks.LocalAppDialogCallbacks
import us.huseli.thoucylinder.dataclasses.track.LocalTrackCallbacks
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.QueueViewModel

@Composable
fun QueueScreen(modifier: Modifier = Modifier, viewModel: QueueViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val dialogCallbacks = LocalAppDialogCallbacks.current

    val selectedTrackCount by viewModel.selectedTrackCount.collectAsStateWithLifecycle()
    val currentComboId by viewModel.currentComboId.collectAsStateWithLifecycle()
    val currentComboIndex by viewModel.currentComboIndex.collectAsStateWithLifecycle()
    val radioUiState by viewModel.radioUiState.collectAsStateWithLifecycle()
    val uiStates by viewModel.trackUiStates.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to -> viewModel.onMoveTrack(from.index, to.index) },
        onDragEnd = { from, to -> viewModel.onMoveTrackFinished(from, to) },
    )
    val scrollbarState = rememberScrollbarListState(listState = reorderableState.listState)
    val trackCallbacks = LocalTrackCallbacks.current.copy(
        onEnqueueClick = { viewModel.enqueueTrack(it.id) },
        onPlayClick = { viewModel.playTrack(it) },
    )

    LaunchedEffect(currentComboIndex) {
        currentComboIndex?.also {
            scrollbarState.scrollToIndexOnLoad(index = it, scrollOffset = -2, key = currentComboId)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        BasicHeader(title = radioUiState?.getFullTitle(context) ?: stringResource(R.string.queue)) {
            if (radioUiState != null) {
                SmallOutlinedButton(
                    onClick = { viewModel.deactivateRadio() },
                    text = stringResource(R.string.deactivate),
                )
            } else if (uiStates.isNotEmpty()) {
                SmallOutlinedButton(
                    onClick = { viewModel.clearQueue() },
                    text = stringResource(R.string.clear),
                )
            }
        }

        CompositionLocalProvider(LocalTrackCallbacks provides trackCallbacks) {
            TrackListReorderable(
                states = { uiStates },
                getDownloadStateFlow = remember { { viewModel.getTrackDownloadUiStateFlow(it) } },
                onClick = remember { { viewModel.onTrackClick(it) } },
                onLongClick = remember { { viewModel.onTrackLongClick(it.id) } },
                selectedTrackCount = { selectedTrackCount },
                trackSelectionCallbacks = viewModel.getTrackSelectionCallbacks(dialogCallbacks),
                reorderableState = reorderableState,
                scrollbarState = scrollbarState,
                showAlbum = false,
                showArtist = true,
                isLoading = isLoading,
                extraBottomSheetItems = { state ->
                    BottomSheetItem(
                        icon = Icons.Sharp.Delete,
                        text = stringResource(R.string.remove_from_queue),
                        onClick = { viewModel.removeFromQueue(state.id) },
                    )
                },
                extraSelectionActions = persistentListOf(
                    SelectionAction(
                        onClick = { viewModel.removeSelectedTracksFromQueue() },
                        icon = Icons.Sharp.Delete,
                        description = R.string.remove,
                    )
                ),
                onEmpty = { Text(text = stringResource(R.string.the_queue_is_empty)) },
                containerColor = { state, isDragging ->
                    when {
                        isDragging -> MaterialTheme.colorScheme.surface
                        state.isSelected -> MaterialTheme.colorScheme.primaryContainer
                        currentComboId == state.id -> MaterialTheme.colorScheme.secondaryContainer
                        else -> Color.Unspecified
                    }
                },
            )
        }
    }
}
