package us.huseli.thoucylinder.dataclasses.radio

import android.content.Context
import androidx.compose.runtime.Immutable
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.enums.RadioType
import us.huseli.thoucylinder.getUmlautifiedString

@Immutable
data class RadioUiState(
    val type: RadioType,
    val title: String?,
) {
    fun getFullTitle(context: Context): String {
        return if (type == RadioType.LIBRARY) context.getUmlautifiedString(R.string.library_radio)
        else context.getUmlautifiedString(R.string.x_x_radio, title, context.getString(type.stringRes))
    }
}
