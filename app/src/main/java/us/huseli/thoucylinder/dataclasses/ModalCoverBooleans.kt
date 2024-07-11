package us.huseli.thoucylinder.dataclasses

import androidx.compose.runtime.Immutable

@Immutable
data class ModalCoverBooleans(
    val canGotoNext: Boolean = false,
    val canPlay: Boolean = false,
    val isLoading: Boolean = false,
    val isPlaying: Boolean = false,
    val isRepeatEnabled: Boolean = false,
    val isShuffleEnabled: Boolean = false,
)
