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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableMap
import us.huseli.thoucylinder.enums.AvailabilityFilter
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.enums.SortOrder
import us.huseli.thoucylinder.dataclasses.pojos.TagPojo
import us.huseli.thoucylinder.stringResource

@Composable
fun <SortParameter : Enum<SortParameter>> ListActions(
    initialSearchTerm: String,
    sortParameter: SortParameter,
    sortOrder: SortOrder,
    sortParameters: ImmutableMap<SortParameter, String>,
    sortDialogTitle: String,
    onSort: (parameter: SortParameter, order: SortOrder) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    showFilterButton: Boolean = true,
    filterButtonSelected: Boolean = false,
    tagPojos: List<TagPojo>? = null,
    selectedTagPojos: List<TagPojo>? = null,
    availabilityFilter: AvailabilityFilter? = null,
    onTagsChange: (List<TagPojo>) -> Unit = {},
    onAvailabilityFilterChange: (AvailabilityFilter) -> Unit = {},
    extraButtons: @Composable () -> Unit = {},
) {
    var isSortDialogOpen by rememberSaveable { mutableStateOf(false) }
    var isFilterDialogOpen by rememberSaveable { mutableStateOf(false) }

    if (isFilterDialogOpen) {
        ListFilterDialog(
            tagPojos = tagPojos,
            selectedTagPojos = selectedTagPojos ?: emptyList(),
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

    Row(
        modifier = modifier.fillMaxWidth(),
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

        if (showFilterButton) {
            InputChip(
                selected = filterButtonSelected,
                onClick = { isFilterDialogOpen = true },
                label = {
                    Icon(
                        imageVector = Icons.Sharp.FilterList,
                        contentDescription = stringResource(R.string.filters),
                        modifier = Modifier.size(18.dp),
                    )
                }
            )
        }

        extraButtons()

        CompactSearchTextField(
            value = initialSearchTerm,
            onSearch = onSearch,
            modifier = Modifier.weight(1f),
            placeholderText = stringResource(R.string.search),
            continuousSearch = true,
        )
    }
}
