package us.huseli.thoucylinder.compose

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import us.huseli.retaintheme.sensibleFormat
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.track.TrackContextMenu
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.pojos.QueueTrackPojo
import us.huseli.thoucylinder.getAverageColor
import us.huseli.thoucylinder.repositories.PlayerRepository
import us.huseli.thoucylinder.viewmodels.QueueViewModel
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@Suppress("AnimateAsStateLabel")
@Composable
fun BoxWithConstraintsScope.ModalCover(
    pojo: QueueTrackPojo,
    trackCallbacks: TrackCallbacks,
    modifier: Modifier = Modifier,
    viewModel: QueueViewModel = hiltViewModel(),
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
) {
    val context = LocalContext.current
    val playbackState by viewModel.playerPlaybackState.collectAsStateWithLifecycle()
    val canGotoNext by viewModel.playerCanGotoNext.collectAsStateWithLifecycle()
    val canPlay by viewModel.playerCanPlay.collectAsStateWithLifecycle()
    val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }
    val endPosition = pojo.track.metadata?.duration?.toLong(DurationUnit.MILLISECONDS)?.takeIf { it > 0 }
    val currentPositionMs by viewModel.playerCurrentPositionMs.collectAsStateWithLifecycle()
    val isPlaying = playbackState == PlayerRepository.PlaybackState.PLAYING

    var isContextMenuShown by rememberSaveable { mutableStateOf(false) }
    var isExpanding by rememberSaveable { mutableStateOf(false) }
    var isCollapsing by rememberSaveable { mutableStateOf(false) }
    val baseBackgroundColor = BottomAppBarDefaults.containerColor
    var backgroundColor by remember { mutableStateOf(baseBackgroundColor) }

    val animationSpec = tween<Dp>(150)
    val boxHeight by animateDpAsState(
        targetValue = if (isExpanded) maxHeight else 80.dp,
        animationSpec = animationSpec,
        finishedListener = {
            isExpanding = false
            isCollapsing = false
        },
    )
    val thumbnailPadding by animateDpAsState(if (isExpanded) 60.dp else 5.dp, animationSpec)
    val tonalElevation by animateDpAsState(if (isExpanded) 0.dp else 3.dp, animationSpec)
    val topRowHeight by animateDpAsState(if (isExpanded) 50.dp else 0.dp, animationSpec)

    val expand = {
        isExpanding = true
        onExpand()
    }

    val collapse = {
        isCollapsing = true
        onCollapse()
    }

    LaunchedEffect(pojo) {
        imageBitmap.value = pojo.getFullImage(context)
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
        color = backgroundColor,
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
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(topRowHeight)
                    .padding(top = 10.dp),
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
                        callbacks = trackCallbacks,
                        isShown = isContextMenuShown,
                        onDismissRequest = { isContextMenuShown = false },
                    )
                }
            }

            Column(modifier = Modifier.weight(1f).padding(vertical = 5.dp)) {
                // Row for thumbnail and collapsed content:
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Thumbnail(
                        modifier = Modifier.padding(horizontal = thumbnailPadding),
                        image = imageBitmap.value,
                        shape = MaterialTheme.shapes.extraSmall,
                        placeholderIcon = Icons.Sharp.MusicNote,
                    )

                    if (!isExpanded || isExpanding || isCollapsing) {
                        Column(modifier = Modifier.weight(1f).padding(start = 5.dp)) {
                            Text(
                                text = pojo.track.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            pojo.artist?.also { artist ->
                                Text(
                                    text = artist,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                                )
                            }
                        }
                    }

                    if (!isExpanded || isExpanding || isCollapsing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            IconButton(
                                onClick = { viewModel.playOrPauseCurrent() },
                                content = {
                                    Icon(
                                        if (isPlaying) Icons.Sharp.Pause else Icons.Sharp.PlayArrow,
                                        if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                                    )
                                },
                                enabled = canPlay,
                            )
                            IconButton(
                                onClick = { viewModel.skipToNext() },
                                content = {
                                    Icon(
                                        imageVector = Icons.Sharp.SkipNext,
                                        contentDescription = stringResource(R.string.next),
                                    )
                                },
                                enabled = canGotoNext,
                            )
                        }
                    }
                }

                if (isExpanded || isExpanding || isCollapsing) {
                    ModalCoverExpandedContent(
                        pojo = pojo,
                        viewModel = viewModel,
                        endPositionMs = endPosition?.toFloat() ?: 0f,
                        canPlay = canPlay,
                        canGotoNext = canGotoNext,
                        isPlaying = isPlaying,
                    )
                }
            }

            if (!isExpanded && !isCollapsing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = endPosition?.let { currentPositionMs / it.toFloat() } ?: 0f,
                )
            }
        }
    }
}


@Composable
fun ModalCoverExpandedContent(
    pojo: QueueTrackPojo,
    viewModel: QueueViewModel,
    endPositionMs: Float,
    canPlay: Boolean,
    canGotoNext: Boolean,
    isPlaying: Boolean,
) {
    val isRepeatEnabled by viewModel.playerIsRepeatEnabled.collectAsStateWithLifecycle()
    val isShuffleEnabled by viewModel.playerIsShuffleEnabled.collectAsStateWithLifecycle()
    val toggleButtonColors = IconButtonDefaults.iconToggleButtonColors(
        checkedContentColor = LocalContentColor.current,
        contentColor = LocalContentColor.current.copy(alpha = 0.5f),
    )
    val sliderInteractionSource = remember { MutableInteractionSource() }

    var isDragging by remember { mutableStateOf(false) }
    var currentPositionMs by rememberSaveable { mutableFloatStateOf(0f) }
    var currentPositionSeconds by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        sliderInteractionSource.interactions.collect { interaction ->
            isDragging = interaction is DragInteraction.Start
        }
    }

    LaunchedEffect(Unit) {
        viewModel.playerCurrentPositionMs.collect { pos ->
            if (!isDragging) currentPositionMs = pos.toFloat()
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(1000)
            currentPositionSeconds++
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { currentPositionMs }.collect {
            if ((it - (currentPositionSeconds * 1000)).absoluteValue > 500)
                currentPositionSeconds = (it / 1000).toInt()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(vertical = 15.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
        ) {
            Text(
                text = pojo.track.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge,
            )
            pojo.artist?.also { artist ->
                Text(
                    text = artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Slider(
                value = currentPositionMs,
                onValueChange = { currentPositionMs = it },
                valueRange = 0f..endPositionMs,
                interactionSource = sliderInteractionSource,
                onValueChangeFinished = { viewModel.seekTo(currentPositionMs.toLong()) },
                modifier = Modifier.height(30.dp),
            )
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = currentPositionSeconds.seconds.sensibleFormat(),
                    style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                )
                Text(
                    text = endPositionMs.toDouble().milliseconds.sensibleFormat(),
                    style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                )
            }
        }

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
                    val description =
                        if (isPlaying) stringResource(R.string.pause)
                        else stringResource(R.string.play)
                    Icon(
                        imageVector = if (isPlaying) Icons.Sharp.Pause else Icons.Sharp.PlayArrow,
                        contentDescription = description,
                        modifier = Modifier.scale(1.75f),
                    )
                },
                enabled = canPlay,
                modifier = Modifier.size(80.dp),
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
