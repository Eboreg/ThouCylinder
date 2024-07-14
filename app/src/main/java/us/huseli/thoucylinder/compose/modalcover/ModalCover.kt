package us.huseli.thoucylinder.compose.modalcover

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.KeyboardArrowDown
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.State
import androidx.compose.runtime.asFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.extensions.distance
import us.huseli.thoucylinder.compose.LocalThemeSizes
import us.huseli.thoucylinder.compose.track.TrackBottomSheetWithButton
import us.huseli.thoucylinder.compose.utils.LargerIconButton
import us.huseli.thoucylinder.dataclasses.ModalCoverBooleans
import us.huseli.thoucylinder.dataclasses.callbacks.PlaybackCallbacks
import us.huseli.thoucylinder.dataclasses.track.LocalTrackCallbacks
import us.huseli.thoucylinder.dataclasses.track.ModalCoverTrackUiState
import us.huseli.thoucylinder.dataclasses.track.ModalCoverTrackUiStateLight
import us.huseli.thoucylinder.viewmodels.ModalCoverViewModel

@Composable
fun ModalCover(
    state: ModalCoverState,
    modifier: Modifier = Modifier,
    viewModel: ModalCoverViewModel = hiltViewModel(),
) {
    val albumArtAverageColor = viewModel.albumArtAverageColor.collectAsStateWithLifecycle()
    val booleans by viewModel.booleans.collectAsStateWithLifecycle()
    val trackUiState by viewModel.trackUiState.collectAsStateWithLifecycle()
    val nextTrackUiState by viewModel.nextTrackUiState.collectAsStateWithLifecycle()
    val previousTrackUiState by viewModel.previousTrackUiState.collectAsStateWithLifecycle()
    val currentProgress = viewModel.currentProgress.collectAsStateWithLifecycle().asFloatState()
    val trackAmplitudes = viewModel.currentAmplitudes.collectAsStateWithLifecycle()
    val playbackCallbacks = remember {
        PlaybackCallbacks(
            playOrPauseCurrent = { viewModel.playOrPauseCurrent() },
            seekToProgress = { viewModel.seekToProgress(it) },
            skipToNext = { viewModel.skipToNext() },
            skipToPrevious = { viewModel.skipToPrevious() },
            skipToStartOrPrevious = { viewModel.skipToStartOrPrevious() },
            toggleRepeat = { viewModel.toggleRepeat() },
            toggleShuffle = { viewModel.toggleShuffle() },
        )
    }

    trackUiState?.also {
        ModalCover(
            state = state,
            booleans = booleans,
            trackUiState = it,
            modifier = modifier,
            playbackCallbacks = playbackCallbacks,
            currentProgress = currentProgress,
            trackAmplitudes = trackAmplitudes,
            nextTrackUiState = nextTrackUiState,
            previousTrackUiState = previousTrackUiState,
            albumArtAverageColor = albumArtAverageColor,
        )
    }
}


@Suppress("AnimateAsStateLabel")
@Composable
fun ModalCover(
    state: ModalCoverState,
    booleans: ModalCoverBooleans,
    trackUiState: ModalCoverTrackUiState,
    playbackCallbacks: PlaybackCallbacks,
    currentProgress: FloatState,
    trackAmplitudes: State<List<Int>>,
    nextTrackUiState: ModalCoverTrackUiStateLight?,
    previousTrackUiState: ModalCoverTrackUiStateLight?,
    albumArtAverageColor: State<Color?>,
    modifier: Modifier = Modifier,
) {
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val baseBackgroundColor = BottomAppBarDefaults.containerColor
    val sizes = LocalThemeSizes.current

    val backgroundColor = remember(albumArtAverageColor.value) {
        albumArtAverageColor.value?.compositeOver(baseBackgroundColor) ?: baseBackgroundColor
    }
    val animatedBackgroundColor by animateColorAsState(backgroundColor, tween<Color>(300))
    val backgroundColorDistance = remember(backgroundColor) { backgroundColor.distance(primaryContainerColor) }
    val trackCallbacks = LocalTrackCallbacks.current.copy(onPlayClick = null, onEnqueueClick = null)

    Surface(
        color = animatedBackgroundColor,
        tonalElevation = 3.dp,
        modifier = modifier.modalCoverContainer(state),
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxHeight()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = state.isCollapsed,
                    onClick = { state.animateToExpanded() },
                )
        ) {
            // Collapse & menu buttons on top row:
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(state.topRowHeight)
                    .padding(horizontal = 10.dp)
                    .padding(top = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LargerIconButton(
                    icon = Icons.Sharp.KeyboardArrowDown,
                    onClick = { state.animateToCollapsed() },
                )
                CompositionLocalProvider(LocalTrackCallbacks provides trackCallbacks) {
                    TrackBottomSheetWithButton(
                        state = trackUiState,
                        buttonSize = sizes.largerIconButton,
                        iconSize = sizes.largerIconButtonIcon,
                    )
                }
            }

            // Album art & titles:
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                AlbumArtAndTitlesSwipeable(
                    state = state,
                    trackUiState = trackUiState,
                    nextTrackUiState = nextTrackUiState,
                    previousTrackUiState = previousTrackUiState,
                    playbackCallbacks = playbackCallbacks,
                    booleans = booleans,
                )
            }

            if (!state.isCollapsed) {
                ExpandedContent(
                    uiBooleans = booleans,
                    endPositionMs = trackUiState.durationMs.toFloat(),
                    amplitudes = trackAmplitudes,
                    containerColor = if (backgroundColorDistance < 0.5f)
                        MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (backgroundColorDistance < 0.5f)
                        MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.primary,
                    progress = currentProgress,
                    maxWidthPx = state.width,
                    backgroundColor = backgroundColor,
                    callbacks = playbackCallbacks,
                    modifier = Modifier.padding(horizontal = 10.dp).padding(bottom = 10.dp),
                )
            }

            if (state.isCollapsed || state.isAnimating) LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().requiredHeight(state.collapsedProgressIndicatorHeight),
                progress = { currentProgress.floatValue },
                drawStopIndicator = {},
            )
        }
    }
}
