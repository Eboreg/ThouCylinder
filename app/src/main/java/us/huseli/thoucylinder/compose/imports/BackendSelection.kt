package us.huseli.thoucylinder.compose.imports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.InputChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.externalcontent.ImportBackend
import us.huseli.thoucylinder.stringResource

@Composable
fun BackendSelection(
    activeBackend: ImportBackend,
    onSelect: (ImportBackend) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ImportBackend.entries.forEach { backend ->
            InputChip(
                selected = activeBackend == backend,
                onClick = { onSelect(backend) },
                label = { Text(stringResource(backend.stringRes)) },
            )
        }
    }
}
