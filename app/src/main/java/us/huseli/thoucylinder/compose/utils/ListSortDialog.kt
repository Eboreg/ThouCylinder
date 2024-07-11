package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowDownward
import androidx.compose.material.icons.sharp.ArrowUpward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableMap
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.enums.SortOrder
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <SortParameter : Enum<SortParameter>> ListSortDialog(
    modifier: Modifier = Modifier,
    sortParameters: ImmutableMap<SortParameter, String>,
    initialSortParameter: SortParameter,
    initialSortOrder: SortOrder,
    title: String,
    onSort: (parameter: SortParameter, order: SortOrder) -> Unit,
    onCancel: () -> Unit,
) {
    var sortParameter by rememberSaveable(initialSortParameter) { mutableStateOf(initialSortParameter) }
    var sortOrder by rememberSaveable(initialSortOrder) { mutableStateOf(initialSortOrder) }
    val filterChipColors = FilterChipDefaults.filterChipColors(iconColor = MaterialTheme.colorScheme.onSurfaceVariant)

    AlertDialog(
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onCancel,
        modifier = modifier,
        confirmButton = {
            SaveButton(
                onClick = { onSort(sortParameter, sortOrder) },
                content = { Text(stringResource(R.string.sort)) },
            )
        },
        title = { Text(text = title) },
        text = {
            Column {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(bottom = 10.dp),
                ) {
                    sortParameters.forEach { (param, label) ->
                        FilterChip(
                            selected = param == sortParameter,
                            onClick = { sortParameter = param },
                            label = { Text(text = label.umlautify()) },
                            colors = filterChipColors,
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    FilterChip(
                        selected = sortOrder == SortOrder.ASCENDING,
                        onClick = { sortOrder = SortOrder.ASCENDING },
                        label = { Text(stringResource(R.string.ascending)) },
                        leadingIcon = {
                            Icon(Icons.Sharp.ArrowDownward, null, modifier = Modifier.size(20.dp))
                        },
                        colors = filterChipColors,
                    )
                    FilterChip(
                        selected = sortOrder == SortOrder.DESCENDING,
                        onClick = { sortOrder = SortOrder.DESCENDING },
                        label = { Text(stringResource(R.string.descending)) },
                        leadingIcon = {
                            Icon(Icons.Sharp.ArrowUpward, null, modifier = Modifier.size(20.dp))
                        },
                        colors = filterChipColors,
                    )
                }
            }
        }
    )
}