package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.Sort
import androidx.compose.material.icons.sharp.ArrowDownward
import androidx.compose.material.icons.sharp.ArrowUpward
import androidx.compose.material.icons.sharp.FilterList
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.AvailabilityFilter
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.SortOrder
import us.huseli.thoucylinder.dataclasses.pojos.TagPojo
import us.huseli.thoucylinder.stringResource

@Composable
fun <SortParameter : Enum<SortParameter>> ListActions(
    modifier: Modifier = Modifier,
    tonalElevation: Dp = 2.dp,
    initialSearchTerm: String,
    sortParameter: SortParameter,
    sortOrder: SortOrder,
    sortParameters: Map<SortParameter, String>,
    sortDialogTitle: String,
    tagPojos: List<TagPojo>,
    selectedTagPojos: List<TagPojo>,
    availabilityFilter: AvailabilityFilter,
    onSort: (parameter: SortParameter, order: SortOrder) -> Unit,
    onSearch: (String) -> Unit,
    onTagsChange: (List<TagPojo>) -> Unit,
    onAvailabilityFilterChange: (AvailabilityFilter) -> Unit,
) {
    var isSortDialogOpen by rememberSaveable { mutableStateOf(false) }

    var isFilterDialogOpen by rememberSaveable { mutableStateOf(false) }

    if (isFilterDialogOpen) {
        ListFilterDialog(
            tagPojos = tagPojos,
            selectedTagPojos = selectedTagPojos,
            onCancelClick = { isFilterDialogOpen = false },
            availabilityFilter = availabilityFilter,
            onAvailabilityFilterChange = onAvailabilityFilterChange,
            onTagsChange = onTagsChange,
        )
    }

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
                        imageVector = Icons.AutoMirrored.Sharp.Sort,
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

            InputChip(
                selected = selectedTagPojos.isNotEmpty() || availabilityFilter != AvailabilityFilter.ALL,
                onClick = { isFilterDialogOpen = true },
                label = {
                    Icon(
                        imageVector = Icons.Sharp.FilterList,
                        contentDescription = stringResource(R.string.tags),
                        modifier = Modifier.size(18.dp),
                    )
                }
            )

            CompactSearchTextField(
                value = initialSearchTerm,
                onSearch = onSearch,
                modifier = Modifier.weight(1f),
                placeholderText = stringResource(R.string.search),
                continuousSearch = true,
            )
        }
    }
}
