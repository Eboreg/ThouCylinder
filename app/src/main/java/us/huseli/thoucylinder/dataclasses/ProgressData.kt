package us.huseli.thoucylinder.dataclasses

import androidx.compose.runtime.Immutable

@Immutable
data class ProgressData(
    val text: String = "",
    val progress: Double = 0.0,
    val isActive: Boolean = false,
)
