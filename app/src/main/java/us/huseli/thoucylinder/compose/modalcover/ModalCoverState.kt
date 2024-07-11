@file:OptIn(ExperimentalFoundationApi::class)

package us.huseli.thoucylinder.compose.modalcover

import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import us.huseli.retaintheme.isInLandscapeMode
import us.huseli.thoucylinder.interfaces.ILogger
import kotlin.math.max

enum class ModalCoverAnchor { Expanded, Collapsed }
enum class ModalCoverTransientStatus { Expanding, Collapsing }

@Composable
fun rememberModalCoverState(
    contentSize: Size,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    decayAnimationSpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay<Float>(),
    collapsedHeight: Dp = 80.dp,
): ModalCoverState {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val isLandscape = isInLandscapeMode()
    var initialAnchor by rememberSaveable(Unit) { mutableStateOf(ModalCoverAnchor.Collapsed) }

    val draggableState = remember {
        AnchoredDraggableState(
            initialValue = initialAnchor,
            positionalThreshold = { it * 0.5f },
            velocityThreshold = { with(density) { 125.dp.toPx() } },
            snapAnimationSpec = SpringSpec(),
            decayAnimationSpec = decayAnimationSpec,
            confirmValueChange = { true },
        )
    }

    val state = remember {
        ModalCoverState(
            initialAnchor = initialAnchor,
            interactionSource = interactionSource,
            draggableState = draggableState,
            density = density,
            scope = scope,
            initialContentSize = contentSize,
            collapsedHeight = collapsedHeight,
            isLandscape = isLandscape,
        )
    }

    val isDragged = state.interactionSource.collectIsDraggedAsState()

    LaunchedEffect(contentSize) {
        state.log("running LaunchedEffect for contentSize=$contentSize")
        state.updateContentSize(contentSize)
    }

    LaunchedEffect(Unit) {
        snapshotFlow { state.currentAnchor }.collect { initialAnchor = it }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { state.draggableState.currentValue }.collect { status ->
            state.log("draggableState.currentValue=$status; draggableState.targetValue=${state.draggableState.targetValue}, state.currentAnchor=${state.currentAnchor}")
            state.setAnchor(status)
        }
    }

    // Set transient status as soon as either drag or animation begins, and don't unset it until they have both ended.
    LaunchedEffect(Unit) {
        snapshotFlow { isDragged.value }.collect { isDragged ->
            if (isDragged) state.setIsTransient(true)
            else if (!state.draggableState.isAnimationRunning) state.setIsTransient(false)
            state.log("isDragged.value=$isDragged; draggableState.currentValue=${state.draggableState.currentValue}, draggableState.targetValue=${state.draggableState.targetValue}, state.currentAnchor=${state.currentAnchor}, state.transientStatus=${state.transientStatus}")
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { state.draggableState.isAnimationRunning }.collect { isAnimating ->
            if (isAnimating) {
                if (state.draggableState.targetValue != state.draggableState.currentValue) state.setIsTransient(true)
            } else if (!isDragged.value) state.setIsTransient(false)
            state.log("draggableState.isAnimationRunning=$isAnimating; draggableState.currentValue=${state.draggableState.currentValue}, draggableState.targetValue=${state.draggableState.targetValue}, state.currentAnchor=${state.currentAnchor}, state.transientStatus=${state.transientStatus}")
        }
    }

    return state
}

@Suppress("MemberVisibilityCanBePrivate")
@Stable
class ModalCoverState(
    val interactionSource: MutableInteractionSource,
    val draggableState: AnchoredDraggableState<ModalCoverAnchor>,
    private val density: Density,
    private val scope: CoroutineScope,
    private val isLandscape: Boolean,
    initialContentSize: Size,
    initialAnchor: ModalCoverAnchor = ModalCoverAnchor.Collapsed,
    val collapsedHeight: Dp = 80.dp,
) : ILogger {
    private val collapsedHeightPx: Float = with(density) { collapsedHeight.toPx() }
    private val horizontalMarginsDp: Dp = 20.dp
    private val horizontalMarginsPx: Float = with(density) { horizontalMarginsDp.toPx() }

    var contentSize: Size by mutableStateOf(initialContentSize)
        private set
    var transientStatus: ModalCoverTransientStatus? by mutableStateOf(null)
        private set
    var currentAnchor: ModalCoverAnchor by mutableStateOf(initialAnchor)
        private set

    val expandProgress: Float by derivedStateOf {
        draggableState.progress(ModalCoverAnchor.Collapsed, ModalCoverAnchor.Expanded)
    }
    val collapseProgress: Float by derivedStateOf { 1f - expandProgress }
    val heightDp: Dp by derivedStateOf { with(density) { contentSize.height.toDp() } }
    val isCollapsing: Boolean by derivedStateOf { transientStatus == ModalCoverTransientStatus.Collapsing }
    val isExpanding: Boolean by derivedStateOf { transientStatus == ModalCoverTransientStatus.Expanding }
    val isAnimating: Boolean by derivedStateOf { isCollapsing || isExpanding }
    val isExpanded: Boolean by derivedStateOf { currentAnchor == ModalCoverAnchor.Expanded && !isAnimating }
    val isCollapsed: Boolean by derivedStateOf { currentAnchor == ModalCoverAnchor.Collapsed && !isAnimating }
    val width: Float by derivedStateOf { contentSize.width - horizontalMarginsPx }
    val widthDp: Dp by derivedStateOf { with(density) { width.toDp() } }
    val albumArtMaxSize: Dp by derivedStateOf {
        if (isLandscape) heightDp - 60.dp - 158.dp
        else (heightDp - 300.dp).coerceAtMost(widthDp - 50.dp)
    }
    val albumArtModelSize: Int by derivedStateOf {
        with(density) {
            if (isLandscape) (heightDp - 60.dp - 158.dp).roundToPx()
            else (widthDp - 50.dp).roundToPx()
        }
    }
    val albumArtSize: Dp by derivedStateOf {
        65.dp + ((albumArtMaxSize - 65.dp) * expandProgress)
    }
    val albumArtContainerWidth: Dp by derivedStateOf {
        if (isLandscape) albumArtSize
        else 65.dp + ((widthDp - 65.dp) * expandProgress)
    }
    val topRowHeight: Dp by derivedStateOf { 60.dp * expandProgress }
    val offset: Float by derivedStateOf {
        draggableState.offset.let { if (it.isNaN()) max(contentSize.height, contentSize.width) else it }
    }
    val collapsedProgressIndicatorHeight: Dp by derivedStateOf { 4.0.dp * collapseProgress }

    @Stable
    private suspend fun animateTo(value: ModalCoverAnchor) {
        if (value != currentAnchor) {
            log("animateTo($value): currentAnchor=$currentAnchor")
            draggableState.animateTo(value)
        }
    }

    @Stable
    fun animateToCollapsed() {
        log("animateToCollapsed()")
        scope.launch { animateTo(ModalCoverAnchor.Collapsed) }
    }

    @Stable
    fun animateToExpanded() {
        log("animateToExpanded()")
        scope.launch { animateTo(ModalCoverAnchor.Expanded) }
    }

    @Stable
    fun setAnchor(value: ModalCoverAnchor) {
        if (currentAnchor != value) {
            log("setAnchor($value), currentAnchor=$currentAnchor")
            currentAnchor = value
        }
    }

    @Stable
    fun setIsTransient(value: Boolean) {
        /**
         * Logic: If value = true and we're not already in a transient state, make it so we are. In other words, if
         * current anchor is Collapsed, we will now start expanding.
         */
        if (value && transientStatus == null) {
            val newTransientStatus = when (currentAnchor) {
                ModalCoverAnchor.Expanded -> ModalCoverTransientStatus.Collapsing
                ModalCoverAnchor.Collapsed -> ModalCoverTransientStatus.Expanding
            }

            log("setIsTransient(true): currentAnchor=$currentAnchor, transientStatus=$transientStatus; setting transientStatus=$newTransientStatus")
            transientStatus = newTransientStatus
        } else if (!value) {
            log("setIsTransient(false): currentAnchor=$currentAnchor, transientStatus=$transientStatus; setting transientStatus=null")
            transientStatus = null
        }
    }

    @Stable
    fun updateContentSize(value: Size) {
        contentSize = value
        draggableState.updateAnchors(
            DraggableAnchors {
                ModalCoverAnchor.Expanded at 0f
                ModalCoverAnchor.Collapsed at value.height - collapsedHeightPx
            },
            currentAnchor,
        )
    }

    override fun log(priority: Int, tag: String, message: String, force: Boolean) {
        if (LOGGING_ENABLED) super.log(priority, tag, "[${System.identityHashCode(this)}] $message", force)
    }

    companion object {
        const val LOGGING_ENABLED = false
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Stable
fun Modifier.modalCoverContainer(state: ModalCoverState) = this
    .fillMaxHeight()
    .layout { measurable, constraints ->
        val offset = state.offset.toInt()
        val placeable = measurable.measure(constraints.offset(0, -offset))

        layout(width = placeable.width, height = placeable.height + offset) {
            state.log("placeable.height=${placeable.height}, offset=$offset")
            placeable.place(0, offset)
        }
    }
    .anchoredDraggable(
        state = state.draggableState,
        orientation = Orientation.Vertical,
        interactionSource = state.interactionSource,
    )
