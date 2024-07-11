package us.huseli.thoucylinder.dataclasses

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

@Immutable
data class ProgressData(
    val text: String = "",
    @StringRes val stringRes: Int? = null,
    val progress: Double = 0.0,
    val isActive: Boolean = false,
)
