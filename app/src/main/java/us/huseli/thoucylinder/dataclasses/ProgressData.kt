package us.huseli.thoucylinder.dataclasses

import androidx.annotation.StringRes
import us.huseli.thoucylinder.R

data class ProgressData(
    val item: String,
    val progress: Double,
    val status: Status,
) {
    enum class Status(@StringRes val stringId: Int) {
        DOWNLOADING(R.string.downloading),
        CONVERTING(R.string.converting),
        MOVING(R.string.moving),
        MATCHING(R.string.matching),
        IMPORTING(R.string.importing),
    }
}
