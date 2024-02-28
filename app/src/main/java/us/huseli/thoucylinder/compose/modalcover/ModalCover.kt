@file:Suppress("DEPRECATION")

package us.huseli.thoucylinder.compose.modalcover

import android.content.res.Configuration
import android.util.Log
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
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.KeyboardArrowDown
import androidx.compose.material.icons.sharp.MoreVert
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.SkipNext
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.rememberSwipeableState
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.swipeable
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.huseli.retaintheme.extensions.distance
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.track.TrackContextMenu
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.combos.QueueTrackCombo
import us.huseli.thoucylinder.getAverageColor
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.QueueViewModel
import kotlin.math.roundToInt
import kotlin.time.DurationUnit

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
@Suppress("AnimateAsStateLabel")
@Composable
fun BoxWithConstraintsScope.ModalCover(
    trackCombo: QueueTrackCombo,
    trackCallbacks: TrackCallbacks<*>,
    modifier: Modifier = Modifier,
    viewModel: QueueViewModel = hiltViewModel(),
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
) {
    val baseBackgroundColor = BottomAppBarDefaults.containerColor
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val dpAnimationSpec = tween<Dp>(150)
    val floatAnimationSpec = tween<Float>(150)
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer

    var backgroundColor by remember { mutableStateOf(baseBackgroundColor) }
    val currentTrackEndPosition by remember(trackCombo.track.trackId) {
        mutableStateOf(trackCombo.track.metadata?.duration?.toLong(DurationUnit.MILLISECONDS)?.takeIf { it > 0 })
    }
    val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }
    var isCollapsing by remember { mutableStateOf(false) }
    var isContextMenuShown by rememberSaveable { mutableStateOf(false) }
    var isExpanding by remember { mutableStateOf(false) }
    val nextImageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }
    var nextTrackCombo by remember { mutableStateOf<QueueTrackCombo?>(null) }
    val previousImageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }
    var previousTrackCombo by remember { mutableStateOf<QueueTrackCombo?>(null) }

    val canGotoNext by viewModel.canGotoNext.collectAsStateWithLifecycle()
    val canPlay by viewModel.canPlay.collectAsStateWithLifecycle(false)
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle(false)

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

    val expand = {
        isExpanding = true
        onExpand()
    }

    val collapse = {
        isCollapsing = true
        onCollapse()
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
            Log.i("ModalCover", "newValue=$newValue")
            when (newValue) {
                "next" -> viewModel.skipToNext()
                "previous" -> viewModel.skipToPrevious()
            }
            true
        },
    )

    LaunchedEffect(isExpanded) {
        if (isExpanded) verticalSwipeState.snapTo("expanded")
    }

    LaunchedEffect(trackCombo.queueTrackId) {
        withContext(Dispatchers.IO) {
            imageBitmap.value = trackCombo.getFullImageBitmap(context)
        }
    }

    LaunchedEffect(trackCombo.queueTrackId) {
        horizontalSwipeState.snapTo("current")
        nextTrackCombo = viewModel.getNextCombo()
        previousTrackCombo = viewModel.getPreviousCombo()
        withContext(Dispatchers.IO) {
            nextImageBitmap.value = nextTrackCombo?.getFullImageBitmap(context)
            previousImageBitmap.value = previousTrackCombo?.getFullImageBitmap(context)
        }
    }

    LaunchedEffect(imageBitmap.value) {
        withContext(Dispatchers.IO) {
            backgroundColor = imageBitmap.value
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
                        isDownloadable = trackCombo.track.isDownloadable,
                        isInLibrary = trackCombo.track.isInLibrary,
                        callbacks = trackCallbacks,
                        isShown = isContextMenuShown,
                        onDismissRequest = { isContextMenuShown = false },
                        trackArtists = trackCombo.artists,
                    )
                }
            }

            FlowRow(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .padding(top = flowRowPadding)
                    .let {
                        if (isExpanded && !isLandscape) {
                            val anchors = mutableMapOf(0f to "current")
                            if (previousTrackCombo != null) anchors[maxWidthPx] = "previous"
                            if (nextTrackCombo != null) anchors[-maxWidthPx] = "next"

                            it.swipeable(
                                state = horizontalSwipeState,
                                anchors = anchors,
                                orientation = Orientation.Horizontal,
                                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                            ).fillMaxHeight()
                        } else if (!isLandscape && (isExpanding || isCollapsing)) it.fillMaxHeight()
                        else if (!isExpanded) it.weight(1f)
                        else it
                    },
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                maxItemsInEachRow = if (isExpanded && !isExpanding && !isCollapsing && !isLandscape) 1 else 3,
            ) {
                val boxModifier =
                    if (isExpanded && !isExpanding && !isCollapsing && !isLandscape)
                        Modifier
                            .offset(x = if (previousTrackCombo != null) -this@ModalCover.maxWidth else 0.dp)
                            .offset { IntOffset(x = horizontalSwipeState.offset.value.roundToInt(), y = 0) }
                            .width(
                                this@ModalCover.maxWidth * (1 + listOfNotNull(nextTrackCombo, previousTrackCombo).size)
                            )
                    else Modifier

                // Album art (+ previous/left album art + titles for portrait):
                Box(modifier = boxModifier) {
                    // Previous track thumbnail & titles:
                    if (isExpanded && !isExpanding && !isLandscape) {
                        previousTrackCombo?.also { combo ->
                            AlbumArtAndTitlesColumn(
                                modifier = Modifier.heightIn(min = this@ModalCover.maxHeight * 0.52f),
                                imageBitmap = previousImageBitmap.value,
                                title = combo.track.title,
                                artist = combo.artists.joined(),
                                animationSpec = dpAnimationSpec,
                                offsetX = 0,
                                contentAlpha = expandedContentAlpha,
                                isExpanded = true,
                                showTitles = true,
                            )
                        }
                    }

                    AlbumArtAndTitlesColumn(
                        modifier = Modifier.heightIn(min = this@ModalCover.maxHeight * 0.52f),
                        imageBitmap = imageBitmap.value,
                        title = trackCombo.track.title,
                        artist = trackCombo.artists.joined(),
                        animationSpec = dpAnimationSpec,
                        offsetX = if (previousTrackCombo != null && !isLandscape && isExpanded && !isExpanding)
                            maxWidthPx.toInt() else 0,
                        contentAlpha = expandedContentAlpha,
                        isExpanded = isExpanded,
                        showTitles = (isExpanded || isCollapsing) && !isLandscape,
                    )

                    // Next track thumbnail & titles:
                    if (isExpanded && !isExpanding && !isLandscape) {
                        nextTrackCombo?.also { combo ->
                            AlbumArtAndTitlesColumn(
                                modifier = Modifier.heightIn(min = this@ModalCover.maxHeight * 0.52f),
                                imageBitmap = nextImageBitmap.value,
                                title = combo.track.title,
                                artist = combo.artists.joined(),
                                animationSpec = dpAnimationSpec,
                                offsetX = maxWidthPx.toInt() * if (previousTrackCombo != null) 2 else 1,
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
                                title = trackCombo.track.title,
                                artist = trackCombo.artists.joined(),
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
                                    onClick = { viewModel.playOrPauseCurrent() },
                                    content = {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.onBackground,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        } else {
                                            Icon(
                                                if (isPlaying) Icons.Sharp.Pause else Icons.Sharp.PlayArrow,
                                                if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                                                modifier = Modifier.size(28.dp),
                                            )
                                        }
                                    },
                                    enabled = canPlay,
                                    modifier = Modifier.size(48.dp),
                                    colors = iconButtonColors,
                                )
                                IconButton(
                                    onClick = { viewModel.skipToNext() },
                                    content = {
                                        Icon(
                                            imageVector = Icons.Sharp.SkipNext,
                                            contentDescription = stringResource(R.string.next),
                                            modifier = Modifier.size(28.dp),
                                        )
                                    },
                                    enabled = canGotoNext,
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
                                    title = trackCombo.track.title,
                                    artist = trackCombo.artists.joined(),
                                    alpha = expandedContentAlpha,
                                )
                            }
                            ExpandedContent(
                                viewModel = viewModel,
                                endPositionMs = currentTrackEndPosition?.toFloat() ?: 0f,
                                canPlay = canPlay,
                                canGotoNext = canGotoNext,
                                isPlaying = isPlaying,
                                isLoading = isLoading,
                                containerColor = if (backgroundColorDistance < 0.45f)
                                    MaterialTheme.colorScheme.tertiaryContainer
                                else MaterialTheme.colorScheme.primaryContainer,
                                contentColor = if (backgroundColorDistance < 0.45f)
                                    MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            if (!isExpanded && !isCollapsing) CollapsedProgressBar(viewModel = viewModel)
        }
    }
}
