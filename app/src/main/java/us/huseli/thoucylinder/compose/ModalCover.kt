package us.huseli.thoucylinder.compose

import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.DragInteraction
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.KeyboardArrowDown
import androidx.compose.material.icons.sharp.MoreVert
import androidx.compose.material.icons.sharp.MusicNote
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.Repeat
import androidx.compose.material.icons.sharp.Shuffle
import androidx.compose.material.icons.sharp.SkipNext
import androidx.compose.material.icons.sharp.SkipPrevious
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.huseli.retaintheme.extensions.sensibleFormat
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.track.TrackContextMenu
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.pojos.QueueTrackPojo
import us.huseli.thoucylinder.distance
import us.huseli.thoucylinder.getAverageColor
import us.huseli.thoucylinder.viewmodels.QueueViewModel
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
@Suppress("AnimateAsStateLabel")
@Composable
fun BoxWithConstraintsScope.ModalCover(
    pojo: QueueTrackPojo,
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
    val currentTrackEndPosition by remember(pojo.track.trackId) {
        mutableStateOf(pojo.track.metadata?.duration?.toLong(DurationUnit.MILLISECONDS)?.takeIf { it > 0 })
    }
    val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }
    var isCollapsing by remember { mutableStateOf(false) }
    var isContextMenuShown by rememberSaveable { mutableStateOf(false) }
    var isExpanding by remember { mutableStateOf(false) }
    val nextImageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }
    var nextPojo by remember { mutableStateOf<QueueTrackPojo?>(null) }
    val previousImageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }
    var previousPojo by remember { mutableStateOf<QueueTrackPojo?>(null) }

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

    LaunchedEffect(pojo.queueTrackId) {
        withContext(Dispatchers.IO) {
            imageBitmap.value = pojo.getFullImageBitmap(context)
        }
    }

    LaunchedEffect(pojo.queueTrackId) {
        horizontalSwipeState.snapTo("current")
        nextPojo = viewModel.getNextPojo()
        previousPojo = viewModel.getPreviousPojo()
        withContext(Dispatchers.IO) {
            nextImageBitmap.value = nextPojo?.getFullImageBitmap(context)
            previousImageBitmap.value = previousPojo?.getFullImageBitmap(context)
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
                        isDownloadable = pojo.track.isDownloadable,
                        isInLibrary = pojo.track.isInLibrary,
                        callbacks = trackCallbacks,
                        isShown = isContextMenuShown,
                        onDismissRequest = { isContextMenuShown = false },
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
                            if (previousPojo != null) anchors[maxWidthPx] = "previous"
                            if (nextPojo != null) anchors[-maxWidthPx] = "next"

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
                            .offset(x = if (previousPojo != null) -this@ModalCover.maxWidth else 0.dp)
                            .offset { IntOffset(x = horizontalSwipeState.offset.value.roundToInt(), y = 0) }
                            .width(this@ModalCover.maxWidth * (1 + listOfNotNull(nextPojo, previousPojo).size))
                    else Modifier

                // Album art (+ previous/left album art + titles for portrait):
                Box(modifier = boxModifier) {
                    // Previous track thumbnail & titles:
                    if (isExpanded && !isExpanding && !isLandscape) {
                        previousPojo?.also { pojo ->
                            AlbumArtAndTitlesColumn(
                                modifier = Modifier.heightIn(min = this@ModalCover.maxHeight * 0.52f),
                                imageBitmap = previousImageBitmap.value,
                                title = pojo.track.title,
                                artist = pojo.artist,
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
                        title = pojo.track.title,
                        artist = pojo.artist,
                        animationSpec = dpAnimationSpec,
                        offsetX = if (previousPojo != null && !isLandscape && isExpanded && !isExpanding)
                            maxWidthPx.toInt() else 0,
                        contentAlpha = expandedContentAlpha,
                        isExpanded = isExpanded,
                        showTitles = (isExpanded || isCollapsing) && !isLandscape,
                    )

                    // Next track thumbnail & titles:
                    if (isExpanded && !isExpanding && !isLandscape) {
                        nextPojo?.also { pojo ->
                            AlbumArtAndTitlesColumn(
                                modifier = Modifier.heightIn(min = this@ModalCover.maxHeight * 0.52f),
                                imageBitmap = nextImageBitmap.value,
                                title = pojo.track.title,
                                artist = pojo.artist,
                                animationSpec = dpAnimationSpec,
                                offsetX = maxWidthPx.toInt() * if (previousPojo != null) 2 else 1,
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
                                title = pojo.track.title,
                                artist = pojo.artist,
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
                                    title = pojo.track.title,
                                    artist = pojo.artist,
                                    alpha = expandedContentAlpha,
                                )
                            }
                            ModalCoverExpandedContent(
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


@Composable
fun CollapsedProgressBar(viewModel: QueueViewModel) {
    val currentProgress by viewModel.currentProgress.collectAsStateWithLifecycle(0f)

    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), progress = currentProgress)
}


@Suppress("AnimateAsStateLabel")
@Composable
fun AlbumArtAndTitlesColumn(
    imageBitmap: ImageBitmap?,
    title: String,
    artist: String?,
    animationSpec: AnimationSpec<Dp>,
    offsetX: Int,
    contentAlpha: Float,
    showTitles: Boolean,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val width by animateDpAsState(
        if (isExpanded && !isLandscape) configuration.screenWidthDp.dp
        else if (isExpanded) 250.dp
        else 66.dp,
        animationSpec,
    )

    Column(
        modifier = modifier.width(width).offset { IntOffset(x = offsetX, y = 0) },
        horizontalAlignment = Alignment.CenterHorizontally,
        // verticalArrangement = Arrangement.spacedBy(15.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        AlbumArtColumn(imageBitmap = imageBitmap)
        if (showTitles) {
            TitlesColumn(isExpanded = true, title = title, artist = artist, alpha = contentAlpha)
        }
    }
}


@Composable
fun AlbumArtColumn(imageBitmap: ImageBitmap?) {
    val configuration = LocalConfiguration.current
    val thumbnailMaxHeight = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 250.dp else 270.dp

    Thumbnail(
        modifier = Modifier.sizeIn(maxHeight = thumbnailMaxHeight),
        image = imageBitmap,
        shape = MaterialTheme.shapes.extraSmall,
        placeholderIcon = Icons.Sharp.MusicNote,
    )
}


@Suppress("AnimateAsStateLabel")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TitlesColumn(modifier: Modifier = Modifier, isExpanded: Boolean, title: String, artist: String?, alpha: Float) {
    val intAnimationSpec = tween<Int>(150)
    val artistTextSize by animateIntAsState(if (isExpanded) 18 else 14, intAnimationSpec)
    val titleTextSize by animateIntAsState(if (isExpanded) 24 else 16, intAnimationSpec)
    val textColor = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha)

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = if (isExpanded) Alignment.CenterHorizontally else Alignment.Start,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = ThouCylinderTheme.typographyExtended.listNormalHeader,
            fontSize = titleTextSize.sp,
            color = textColor,
            modifier = Modifier.basicMarquee(Int.MAX_VALUE),
        )
        if (artist != null) {
            Text(
                text = artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = ThouCylinderTheme.typographyExtended.listNormalSubtitle,
                fontSize = artistTextSize.sp,
                modifier = Modifier.basicMarquee(Int.MAX_VALUE),
                color = textColor,
            )
        }
    }
}


@Composable
fun ModalCoverExpandedContent(
    viewModel: QueueViewModel,
    endPositionMs: Float,
    canPlay: Boolean,
    canGotoNext: Boolean,
    isPlaying: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.primary,
) {
    val isRepeatEnabled by viewModel.isRepeatEnabled.collectAsStateWithLifecycle()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsStateWithLifecycle()
    val toggleButtonColors = IconButtonDefaults.iconToggleButtonColors(
        checkedContentColor = LocalContentColor.current,
        contentColor = LocalContentColor.current.copy(alpha = 0.5f),
    )

    Column(
        modifier = modifier.padding(top = 15.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        ExpandedProgressBar(
            viewModel = viewModel,
            containerColor = containerColor,
            contentColor = contentColor,
            endPositionMs = endPositionMs,
        )

        // Large button row:
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconToggleButton(
                checked = isShuffleEnabled,
                onCheckedChange = { viewModel.toggleShuffle() },
                content = { Icon(Icons.Sharp.Shuffle, null) },
                colors = toggleButtonColors,
            )
            IconButton(
                onClick = { viewModel.skipToStartOrPrevious() },
                content = {
                    Icon(
                        imageVector = Icons.Sharp.SkipPrevious,
                        contentDescription = null,
                        modifier = Modifier.scale(1.5f),
                    )
                },
                modifier = Modifier.size(60.dp),
            )
            FilledTonalIconButton(
                onClick = { viewModel.playOrPauseCurrent() },
                content = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = contentColor,
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        val description =
                            if (isPlaying) stringResource(R.string.pause)
                            else stringResource(R.string.play)
                        Icon(
                            imageVector = if (isPlaying) Icons.Sharp.Pause else Icons.Sharp.PlayArrow,
                            contentDescription = description,
                            modifier = Modifier.scale(1.75f),
                        )
                    }
                },
                enabled = canPlay,
                modifier = Modifier.size(80.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = containerColor,
                    contentColor = contentColor,
                ),
            )
            IconButton(
                onClick = { viewModel.skipToNext() },
                content = {
                    Icon(
                        imageVector = Icons.Sharp.SkipNext,
                        contentDescription = stringResource(R.string.next),
                        modifier = Modifier.scale(1.5f),
                    )
                },
                enabled = canGotoNext,
                modifier = Modifier.size(60.dp),
            )
            IconToggleButton(
                checked = isRepeatEnabled,
                onCheckedChange = { viewModel.toggleRepeat() },
                content = { Icon(Icons.Sharp.Repeat, null) },
                colors = toggleButtonColors,
            )
        }
    }
}


@Composable
fun ExpandedProgressBar(
    viewModel: QueueViewModel,
    endPositionMs: Float,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.primary,
) {
    // val currentPositionSeconds by viewModel.currentPositionSeconds.collectAsStateWithLifecycle(0)
    var currentPositionSeconds by remember { mutableLongStateOf(0L) }
    var isDragging by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    val sliderInteractionSource = remember { MutableInteractionSource() }

    LaunchedEffect(Unit) {
        sliderInteractionSource.interactions.collect { interaction ->
            isDragging = interaction is DragInteraction.Start
        }
    }

    LaunchedEffect(Unit) {
        viewModel.currentProgress.collect { progress ->
            if (!isDragging) currentProgress = progress
        }
    }

    LaunchedEffect(Unit) {
        viewModel.currentPositionSeconds.collect { pos ->
            if (!isDragging) currentPositionSeconds = pos
        }
    }

    Slider(
        value = currentProgress,
        interactionSource = sliderInteractionSource,
        modifier = modifier.padding(horizontal = 10.dp).height(30.dp),
        colors = SliderDefaults.colors(
            thumbColor = contentColor,
            activeTrackColor = contentColor,
            inactiveTrackColor = containerColor,
        ),
        onValueChange = {
            currentProgress = it
            currentPositionSeconds = ((endPositionMs / 1000) * it).toLong()
        },
        onValueChangeFinished = { viewModel.seekToProgress(currentProgress) },
    )
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = currentPositionSeconds.seconds.sensibleFormat(),
            style = ThouCylinderTheme.typographyExtended.listSmallTitle,
        )
        Text(
            text = endPositionMs.toDouble().milliseconds.sensibleFormat(),
            style = ThouCylinderTheme.typographyExtended.listSmallTitle,
        )
    }
}
