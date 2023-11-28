package us.huseli.thoucylinder.dataclasses

import androidx.annotation.StringRes
import us.huseli.thoucylinder.R

enum class ImportProgressStatus(@StringRes val stringId: Int) {
    MATCHING(R.string.matching),
    IMPORTING(R.string.importing),
}

data class ImportProgressData(
    val item: String,
    val progress: Double,
    val status: ImportProgressStatus,
)
