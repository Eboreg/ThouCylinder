package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ViewList
import androidx.compose.material.icons.sharp.GridView
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R

enum class DisplayType { LIST, GRID }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayTypeSelection(
    displayType: DisplayType,
    onDisplayTypeChange: (DisplayType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        InputChip(
            modifier = Modifier.padding(horizontal = 10.dp),
            selected = displayType == DisplayType.LIST,
            onClick = { onDisplayTypeChange(DisplayType.LIST) },
            label = { Text(text = stringResource(R.string.list)) },
            leadingIcon = { Icon(imageVector = Icons.AutoMirrored.Sharp.ViewList, contentDescription = null) },
        )
        InputChip(
            modifier = Modifier.padding(horizontal = 10.dp),
            selected = displayType == DisplayType.GRID,
            onClick = { onDisplayTypeChange(DisplayType.GRID) },
            label = { Text(text = stringResource(R.string.grid)) },
            leadingIcon = { Icon(imageVector = Icons.Sharp.GridView, contentDescription = null) },
        )
    }
}
