package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowDownward
import androidx.compose.material.icons.sharp.ArrowUpward
import androidx.compose.material.icons.sharp.Clear
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material.icons.sharp.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.SortOrder

@Composable
fun <SortParameter : Enum<SortParameter>> ListActions(
    modifier: Modifier = Modifier,
    tonalElevation: Dp = 2.dp,
    initialSearchTerm: String,
    sortParameter: SortParameter,
    sortOrder: SortOrder,
    sortParameters: Map<SortParameter, String>,
    sortDialogTitle: String,
    onSort: (parameter: SortParameter, order: SortOrder) -> Unit,
    onSearch: (String) -> Unit,
) {
    var isSortDialogOpen by rememberSaveable { mutableStateOf(false) }
    var searchTerm by rememberSaveable(initialSearchTerm) { mutableStateOf(initialSearchTerm) }
    var isSearchFocused by rememberSaveable { mutableStateOf(false) }

    if (isSortDialogOpen) {
        ListSortDialog(
            sortParameters = sortParameters,
            initialSortParameter = sortParameter,
            initialSortOrder = sortOrder,
            title = sortDialogTitle,
            onSort = { parameter, order ->
                onSort(parameter, order)
                isSortDialogOpen = false
            },
            onCancel = { isSortDialogOpen = false },
        )
    }

    Surface(
        color = BottomAppBarDefaults.containerColor,
        tonalElevation = tonalElevation,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).padding(bottom = 10.dp, top = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            sortParameters[sortParameter]?.also { label ->
                FilledTonalButton(
                    onClick = { isSortDialogOpen = true },
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Sharp.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(25.dp).padding(end = 5.dp),
                    )
                    Text(text = label, modifier = Modifier.padding(end = 5.dp))
                    Icon(
                        imageVector =
                        if (sortOrder == SortOrder.ASCENDING) Icons.Sharp.ArrowDownward
                        else Icons.Sharp.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            BasicTextField(
                value = searchTerm,
                onValueChange = {
                    searchTerm = it
                    onSearch(it)
                },
                modifier = Modifier
                    .height(32.dp)
                    .padding(0.dp)
                    .weight(1f)
                    .onFocusChanged { isSearchFocused = it.isFocused },
                singleLine = true,
                textStyle = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                decorationBox = { innerTextField ->
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Sharp.Search,
                                contentDescription = null,
                                modifier = Modifier.size(25.dp),
                                tint = MaterialTheme.colorScheme.outline,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxHeight().weight(1f),
                                content = { innerTextField() },
                            )
                            if (searchTerm.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Sharp.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.size(25.dp).clickable {
                                        searchTerm = ""
                                        onSearch("")
                                    },
                                    tint = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                        Divider(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (isSearchFocused) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun <SortParameter : Enum<SortParameter>> ListSortDialog(
    modifier: Modifier = Modifier,
    sortParameters: Map<SortParameter, String>,
    initialSortParameter: SortParameter,
    initialSortOrder: SortOrder,
    title: String,
    onSort: (parameter: SortParameter, order: SortOrder) -> Unit,
    onCancel: () -> Unit,
) {
    var sortParameter by rememberSaveable(initialSortParameter) { mutableStateOf(initialSortParameter) }
    var sortOrder by rememberSaveable(initialSortOrder) { mutableStateOf(initialSortOrder) }

    AlertDialog(
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onCancel,
        modifier = modifier,
        confirmButton = {
            TextButton(
                onClick = { onSort(sortParameter, sortOrder) },
                content = { Text(stringResource(R.string.sort)) },
            )
        },
        title = { Text(text = title) },
        text = {
            Column {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(bottom = 10.dp),
                ) {
                    sortParameters.forEach { (param, label) ->
                        FilterChip(
                            selected = param == sortParameter,
                            onClick = { sortParameter = param },
                            label = { Text(text = label) },
                        )
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    FilterChip(
                        selected = sortOrder == SortOrder.ASCENDING,
                        onClick = { sortOrder = SortOrder.ASCENDING },
                        label = { Text(stringResource(R.string.ascending)) },
                        leadingIcon = {
                            Icon(Icons.Sharp.ArrowDownward, null, modifier = Modifier.size(20.dp))
                        },
                    )
                    FilterChip(
                        selected = sortOrder == SortOrder.DESCENDING,
                        onClick = { sortOrder = SortOrder.DESCENDING },
                        label = { Text(stringResource(R.string.descending)) },
                        leadingIcon = {
                            Icon(Icons.Sharp.ArrowUpward, null, modifier = Modifier.size(20.dp))
                        },
                    )
                }
            }
        }
    )
}
