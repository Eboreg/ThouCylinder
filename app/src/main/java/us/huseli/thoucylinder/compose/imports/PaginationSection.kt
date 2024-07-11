package us.huseli.thoucylinder.compose.imports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.SkipNext
import androidx.compose.material.icons.sharp.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.SmallOutlinedButton
import us.huseli.thoucylinder.stringResource

@Composable
fun PaginationSection(
    currentItemCount: Int?,
    offset: Int,
    totalItemCount: Int?,
    isTotalItemCountExact: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmallOutlinedButton(
            onClick = onPreviousClick,
            content = { Icon(Icons.Sharp.SkipPrevious, stringResource(R.string.previous)) },
            enabled = hasPrevious,
        )
        if (currentItemCount != null && currentItemCount > 0) {
            val prefix = if (isTotalItemCountExact) "" else "≥ "
            Text(text = "${offset + 1} - ${currentItemCount + offset} ($prefix${totalItemCount ?: "?"})")
        }
        SmallOutlinedButton(
            onClick = onNextClick,
            content = { Icon(Icons.Sharp.SkipNext, stringResource(R.string.next)) },
            enabled = hasNext,
        )
    }
}
