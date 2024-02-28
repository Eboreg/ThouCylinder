package us.huseli.thoucylinder.compose.screens.imports

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.stringResource

@Composable
fun SelectAllChip(
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InputChip(
        selected = selected,
        onClick = onClick,
        label = { Icon(Icons.Sharp.SelectAll, stringResource(R.string.select_all)) },
        modifier = modifier,
        enabled = enabled,
    )
}