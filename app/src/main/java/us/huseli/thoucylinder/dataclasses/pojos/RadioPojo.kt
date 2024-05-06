package us.huseli.thoucylinder.dataclasses.pojos

import androidx.compose.runtime.Immutable
import us.huseli.thoucylinder.enums.RadioType

@Immutable
data class RadioPojo(
    val type: RadioType,
    val title: String?,
)
