package us.huseli.thoucylinder.compose.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.InputChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.externalcontent.ExternalListType
import us.huseli.thoucylinder.stringResource

@Composable
fun ListTypeSection(
    current: ExternalListType,
    onSelect: (ExternalListType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ExternalListType.entries.forEach { listType ->
            InputChip(
                selected = current == listType,
                onClick = { onSelect(listType) },
                label = { Text(stringResource(listType.stringRes)) },
            )
        }
    }
}
