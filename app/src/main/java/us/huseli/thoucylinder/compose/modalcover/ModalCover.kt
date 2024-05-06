@file:Suppress("DEPRECATION", "UsingMaterialAndMaterial3Libraries")

package us.huseli.thoucylinder.compose.modalcover

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.KeyboardArrowDown
import androidx.compose.material.icons.sharp.MoreVert
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.SkipNext
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.huseli.retaintheme.extensions.distance
import us.huseli.thoucylinder.Logger
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.track.TrackContextMenu
import us.huseli.thoucylinder.dataclasses.callbacks.PlaybackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.uistates.ModalCoverTrackUiState
import us.huseli.thoucylinder.dataclasses.uistates.ModalCoverTrackUiStateLight
import us.huseli.thoucylinder.getAverageColor
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.QueueViewModel
import kotlin.math.roundToInt

@Composable
fun BoxWithConstraintsScope.ModalCover(
    trackCallbacks: TrackCallbacks,
    modifier: Modifier = Modifier,
    viewModel: QueueViewModel = hiltViewModel(),
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    isExpanded: Boolean = false,
) {
    val uiState by viewModel.modalCoverTrackUiState.collectAsStateWithLifecycle()
    val nextUiState = viewModel.modalCoverNextTrackUiState.collectAsStateWithLifecycle()
    val previousUiState = viewModel.modalCoverPreviousTrackUiState.collectAsStateWithLifecycle()
    val canGotoNext = viewModel.canGotoNext.collectAsStateWithLifecycle()
    val canPlay = viewModel.canPlay.collectAsStateWithLifecycle()
    val isLoading = viewModel.isLoading.collectAsStateWithLifecycle()
    val isPlaying = viewModel.isPlaying.collectAsStateWithLifecycle()
    val isRepeatEnabled = viewModel.isRepeatEnabled.collectAsStateWithLifecycle()
    val isShuffleEnabled = viewModel.isShuffleEnabled.collectAsStateWithLifecycle()
    val currentProgress = viewModel.currentProgress.collectAsStateWithLifecycle()
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

    uiState?.also {
        ModalCover(
            uiState = it,
            trackCallbacks = trackCallbacks,
            modifier = modifier,
            onExpand = onExpand,
            onCollapse = onCollapse,
            playbackCallbacks = playbackCallbacks,
            isExpanded = isExpanded,
            canGotoNext = canGotoNext,
            canPlay = canPlay,
            isLoading = isLoading,
            isPlaying = isPlaying,
            isRepeatEnabled = isRepeatEnabled,
            isShuffleEnabled = isShuffleEnabled,
            currentProgress = currentProgress,
            trackAmplitudes = trackAmplitudes,
            nextUiState = nextUiState,
            previousUiState = previousUiState,
        )
    }
}


@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
@Suppress("AnimateAsStateLabel")
@Composable
fun BoxWithConstraintsScope.ModalCover(
    uiState: ModalCoverTrackUiState,
    trackCallbacks: TrackCallbacks,
    modifier: Modifier = Modifier,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    playbackCallbacks: PlaybackCallbacks,
    isExpanded: Boolean = false,
    canGotoNext: State<Boolean>,
    canPlay: State<Boolean>,
    isLoading: State<Boolean>,
    isPlaying: State<Boolean>,
    isRepeatEnabled: State<Boolean>,
    isShuffleEnabled: State<Boolean>,
    currentProgress: State<Float>,
    trackAmplitudes: State<ImmutableList<Int>>,
    nextUiState: State<ModalCoverTrackUiStateLight?>,
    previousUiState: State<ModalCoverTrackUiStateLight?>,
) {
    val baseBackgroundColor = BottomAppBarDefaults.containerColor
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val dpAnimationSpec = tween<Dp>(150)
    val floatAnimationSpec = tween<Float>(150)
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer

    var backgroundColor by remember { mutableStateOf(baseBackgroundColor) }
    val currentTrackEndPosition by remember(uiState.trackId) {
        mutableStateOf(uiState.durationMs?.takeIf { it > 0 })
    }
    var isCollapsing by remember { mutableStateOf(false) }
    var isContextMenuShown by rememberSaveable { mutableStateOf(false) }
    var isExpanding by remember { mutableStateOf(false) }
    val animatedBackgroundColor by animateColorAsState(backgroundColor)
    val backgroundColorDistance by remember(backgroundColor) {
        mutableFloatStateOf(backgroundColor.distance(primaryContainerColor))
    }
    val boxHeight by animateDpAsState(
        targetValue = if (isExpanded) maxHeight else 80.dp,
        animationSpec = dpAnimationSpec,
        finishedListener = {
            isExpanding = false
            isCollapsing = false
        },
    )
    val collapsedContentAlpha by animateFloatAsState(if (!isExpanded) 1f else 0f, floatAnimationSpec)
    val collapsedTitlesHeight by animateDpAsState(if (isExpanded) 0.dp else 66.dp, dpAnimationSpec)
    val expandedContentAlpha by animateFloatAsState(if (isExpanded) 1f else 0f, floatAnimationSpec)
    val maxWidthPx by remember { mutableFloatStateOf(with(density) { maxWidth.toPx() }) }
    val tonalElevation by animateDpAsState(if (isExpanded) 0.dp else 3.dp, dpAnimationSpec)
    val topRowHeight by animateDpAsState(if (isExpanded) 60.dp else 0.dp, dpAnimationSpec)
    val verticalSwipeEndAnchor by remember { mutableFloatStateOf(with(density) { maxWidthPx - 80.dp.toPx() }) }

    val flowRowPadding by remember(boxHeight, topRowHeight) {
        mutableStateOf(
            if (isLandscape) max((boxHeight - 250.dp) / 2 - topRowHeight, 0.dp)
            else 0.dp
        )
    }

    val expand = remember {
        {
            isExpanding = true
            onExpand()
        }
    }

    val collapse = remember {
        {
            isCollapsing = true
            onCollapse()
        }
    }

    val verticalSwipeState = rememberSwipeableState(
        initialValue = "expanded",
        confirmStateChange = { newValue ->
            if (newValue == "collapsed") collapse()
            true
        },
    )

    val horizontalSwipeState = rememberSwipeableState(
        initialValue = "current",
        confirmStateChange = { newValue ->
            // viewModel.log("ModalCover", "newValue=$newValue")
            when (newValue) {
                "next" -> playbackCallbacks.skipToNext()
                "previous" -> playbackCallbacks.skipToPrevious()
            }
            true
        },
    )

    LaunchedEffect(isExpanded) {
        if (isExpanded) verticalSwipeState.snapTo("expanded")
    }

    LaunchedEffect(uiState.trackId) {
        horizontalSwipeState.snapTo("current")
    }

    LaunchedEffect(uiState.fullImage) {
        withContext(Dispatchers.IO) {
            backgroundColor = uiState.fullImage
                ?.getAverageColor()
                ?.copy(alpha = 0.3f)
                ?.compositeOver(baseBackgroundColor)
                ?: baseBackgroundColor
        }
    }

    Surface(
        color = animatedBackgroundColor,
        tonalElevation = tonalElevation,
        modifier = modifier
            .height(boxHeight)
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !isExpanded,
                onClick = expand,
            )
            .let {
                if (isExpanded || isCollapsing || isExpanding) {
                    it.swipeable(
                        state = verticalSwipeState,
                        anchors = mapOf(0f to "expanded", verticalSwipeEndAnchor to "collapsed"),
                        orientation = Orientation.Vertical,
                        thresholds = { _, _ -> FractionalThreshold(0.5f) },
                    ).offset {
                        IntOffset(
                            0,
                            if (verticalSwipeState.currentValue == "collapsed") 0
                            else verticalSwipeState.offset.value.roundToInt(),
                        )
                    }
                } else it
            },
    ) {
        Column {
            // Collapse & menu buttons on top row:
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(topRowHeight)
                    .padding(horizontal = 10.dp)
                    .padding(top = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = collapse,
                    content = { Icon(Icons.Sharp.KeyboardArrowDown, null) },
                )
                Column {
                    IconButton(
                        onClick = { isContextMenuShown = !isContextMenuShown },
                        content = { Icon(Icons.Sharp.MoreVert, null) },
                    )
                    TrackContextMenu(
                        trackId = uiState.trackId,
                        artists = uiState.artists,
                        isShown = isContextMenuShown,
                        isDownloadable = uiState.isDownloadable,
                        isInLibrary = uiState.isInLibrary,
                        callbacks = trackCallbacks,
                        onDismissRequest = { isContextMenuShown = false },
                        youtubeWebUrl = uiState.youtubeWebUrl,
                        spotifyWebUrl = uiState.spotifyWebUrl,
                        isPlayable = uiState.isPlayable,
                    )
                }
            }

            FlowRow(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .padding(top = flowRowPadding)
                    .let {
                        if (!isLandscape && isExpanded) it.fillMaxHeight()
                        else if (!isExpanded) it.weight(1f)
                        else it
                    },
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                maxItemsInEachRow = if (isExpanded && !isExpanding && !isCollapsing && !isLandscape) 1 else 3,
            ) {
                var boxModifier: Modifier = Modifier

                if (isExpanded && !isLandscape) {
                    val anchors = mutableMapOf(0f to "current")
                    if (previousUiState.value != null) anchors[maxWidthPx] = "previous"
                    if (nextUiState.value != null) anchors[-maxWidthPx] = "next"

                    boxModifier = boxModifier.swipeable(
                        state = horizontalSwipeState,
                        anchors = anchors,
                        orientation = Orientation.Horizontal,
                        thresholds = { _, _ -> FractionalThreshold(0.5f) },
                    )

                    if (!isCollapsing && !isExpanding) {
                        boxModifier = boxModifier
                            .offset(x = if (previousUiState.value != null) -this@ModalCover.maxWidth else 0.dp)
                            .offset {
                                val x = horizontalSwipeState.offset.value.roundToInt()
                                Logger.log("ModalCover", "boxModifier.offset x = $x")
                                IntOffset(x = x, y = 0)
                            }
                            .width(
                                this@ModalCover.maxWidth * (1 + listOfNotNull(nextUiState, previousUiState).size)
                            )
                    }
                }

                // Album art (+ previous/left album art + titles for portrait):
                Box(modifier = boxModifier) {
                    // Previous track thumbnail & titles:
                    if (isExpanded && !isExpanding && !isLandscape) {
                        previousUiState.value?.also { state ->
                            AlbumArtAndTitlesColumn(
                                modifier = Modifier.heightIn(min = this@ModalCover.maxHeight * 0.52f),
                                imageBitmap = { state.fullImage },
                                title = state.title,
                                artist = state.trackArtistString,
                                offsetX = 0,
                                contentAlpha = expandedContentAlpha,
                                isExpanded = true,
                                showTitles = true,
                            )
                        }
                    }

                    AlbumArtAndTitlesColumn(
                        modifier = Modifier.heightIn(min = this@ModalCover.maxHeight * 0.52f),
                        imageBitmap = { uiState.fullImage },
                        title = uiState.title,
                        artist = uiState.artistString,
                        offsetX = if (previousUiState.value != null && !isLandscape && isExpanded && !isExpanding)
                            maxWidthPx.toInt() else 0,
                        contentAlpha = expandedContentAlpha,
                        isExpanded = isExpanded,
                        showTitles = (isExpanded || isCollapsing) && !isLandscape,
                    )

                    // Next track thumbnail & titles:
                    if (isExpanded && !isExpanding && !isLandscape) {
                        nextUiState.value?.also { state ->
                            AlbumArtAndTitlesColumn(
                                modifier = Modifier.heightIn(min = this@ModalCover.maxHeight * 0.52f),
                                imageBitmap = { state.fullImage },
                                title = state.title,
                                artist = state.trackArtistString,
                                offsetX = maxWidthPx.toInt() * if (previousUiState.value != null) 2 else 1,
                                contentAlpha = expandedContentAlpha,
                                isExpanded = true,
                                showTitles = true,
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f).let {
                        if (isLandscape && (isExpanded || isCollapsing || isExpanding)) it.height(250.dp) else it
                    },
                ) {
                    // Titles & buttons for collapsed:
                    if (!isExpanded || isExpanding) {
                        Row(
                            modifier = Modifier.height(collapsedTitlesHeight),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TitlesColumn(
                                modifier = Modifier.weight(1f),
                                isExpanded = false,
                                title = uiState.title,
                                artist = uiState.artistString,
                                alpha = collapsedContentAlpha,
                            )
                            // Buttons:
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                val iconButtonColors = IconButtonDefaults.iconButtonColors(
                                    contentColor = LocalContentColor.current.copy(alpha = collapsedContentAlpha),
                                )

                                IconButton(
                                    onClick = playbackCallbacks.playOrPauseCurrent,
                                    content = {
                                        if (isLoading.value) {
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.onBackground,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        } else {
                                            Icon(
                                                if (isPlaying.value) Icons.Sharp.Pause else Icons.Sharp.PlayArrow,
                                                if (isPlaying.value) stringResource(R.string.pause)
                                                else stringResource(R.string.play),
                                                modifier = Modifier.size(28.dp),
                                            )
                                        }
                                    },
                                    enabled = canPlay.value,
                                    modifier = Modifier.size(48.dp),
                                    colors = iconButtonColors,
                                )
                                IconButton(
                                    onClick = playbackCallbacks.skipToNext,
                                    content = {
                                        Icon(
                                            imageVector = Icons.Sharp.SkipNext,
                                            contentDescription = stringResource(R.string.next),
                                            modifier = Modifier.size(28.dp),
                                        )
                                    },
                                    enabled = canGotoNext.value,
                                    modifier = Modifier.size(48.dp),
                                    colors = iconButtonColors,
                                )
                            }
                        }
                    }

                    if (isExpanded || isCollapsing || isExpanding) {
                        Column(
                            modifier = if (isLandscape) Modifier.weight(1f).height(250.dp) else Modifier,
                            verticalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            if (isLandscape) {
                                TitlesColumn(
                                    isExpanded = true,
                                    title = uiState.title,
                                    artist = uiState.artistString,
                                    alpha = expandedContentAlpha,
                                )
                            }
                            ExpandedContent(
                                endPositionMs = currentTrackEndPosition?.toFloat() ?: 0f,
                                canPlay = canPlay,
                                canGotoNext = canGotoNext,
                                isPlaying = isPlaying,
                                isLoading = isLoading,
                                amplitudes = trackAmplitudes,
                                containerColor = if (backgroundColorDistance < 0.45f)
                                    MaterialTheme.colorScheme.tertiaryContainer
                                else MaterialTheme.colorScheme.primaryContainer,
                                contentColor = if (backgroundColorDistance < 0.45f)
                                    MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.primary,
                                isRepeatEnabled = isRepeatEnabled,
                                isShuffleEnabled = isShuffleEnabled,
                                progress = currentProgress,
                                maxWidthPx = maxWidthPx,
                                backgroundColor = { backgroundColor },
                                callbacks = playbackCallbacks,
                            )
                        }
                    }
                }
            }
            if (!isExpanded && !isCollapsing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), progress = { currentProgress.value })
            }
        }
    }
}
