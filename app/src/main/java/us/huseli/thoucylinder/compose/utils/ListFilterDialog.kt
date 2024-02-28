package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ExpandLess
import androidx.compose.material.icons.sharp.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import us.huseli.thoucylinder.AvailabilityFilter
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.pojos.TagPojo
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListFilterDialog(
    tagPojos: List<TagPojo>,
    selectedTagPojos: List<TagPojo>,
    availabilityFilter: AvailabilityFilter,
    modifier: Modifier = Modifier,
    onCancelClick: () -> Unit,
    onAvailabilityFilterChange: (AvailabilityFilter) -> Unit,
    onTagsChange: (List<TagPojo>) -> Unit,
) {
    var localSelectedTagPojos by rememberSaveable { mutableStateOf(selectedTagPojos) }
    var isTagSectionExpanded by rememberSaveable { mutableStateOf(true) }
    var localAvailabilityFilter by rememberSaveable { mutableStateOf(availabilityFilter) }

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onCancelClick,
        confirmButton = { TextButton(onClick = onCancelClick, content = { Text(stringResource(R.string.close)) }) },
        modifier = modifier.padding(10.dp),
        title = { Text(stringResource(R.string.filters)) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                item { Text(stringResource(R.string.availability)) }
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        FilterChip(
                            selected = localAvailabilityFilter == AvailabilityFilter.ALL,
                            onClick = {
                                localAvailabilityFilter = AvailabilityFilter.ALL
                                onAvailabilityFilterChange(localAvailabilityFilter)
                            },
                            label = { Text(stringResource(AvailabilityFilter.ALL.stringRes)) },
                        )
                        FilterChip(
                            selected = localAvailabilityFilter == AvailabilityFilter.ONLY_PLAYABLE,
                            onClick = {
                                localAvailabilityFilter = AvailabilityFilter.ONLY_PLAYABLE
                                onAvailabilityFilterChange(localAvailabilityFilter)
                            },
                            label = { Text(stringResource(AvailabilityFilter.ONLY_PLAYABLE.stringRes)) },
                        )
                        FilterChip(
                            selected = localAvailabilityFilter == AvailabilityFilter.ONLY_LOCAL,
                            onClick = {
                                localAvailabilityFilter = AvailabilityFilter.ONLY_LOCAL
                                onAvailabilityFilterChange(localAvailabilityFilter)
                            },
                            label = { Text(stringResource(AvailabilityFilter.ONLY_LOCAL.stringRes)) },
                        )
                    }
                }

                if (tagPojos.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.tags), modifier = Modifier.weight(1f))
                            IconButton(onClick = { isTagSectionExpanded = !isTagSectionExpanded }) {
                                Icon(if (isTagSectionExpanded) Icons.Sharp.ExpandLess else Icons.Sharp.ExpandMore, null)
                            }
                        }
                    }
                    if (isTagSectionExpanded) {
                        item {
                            SmallOutlinedButton(
                                onClick = {
                                    localSelectedTagPojos = emptyList()
                                    onTagsChange(localSelectedTagPojos)
                                },
                                text = stringResource(R.string.reset),
                            )
                        }
                        tagPojos.chunked(10).forEach { chunk ->
                            item {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    chunk.forEach { pojo ->
                                        FilterChip(
                                            selected = localSelectedTagPojos.contains(pojo),
                                            onClick = {
                                                localSelectedTagPojos = localSelectedTagPojos.toMutableList().apply {
                                                    if (contains(pojo)) remove(pojo)
                                                    else add(pojo)
                                                }
                                                onTagsChange(localSelectedTagPojos)
                                            },
                                            label = {
                                                Text(pojo.name.umlautify())
                                                Badge(
                                                    modifier = Modifier.padding(start = 5.dp),
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    content = { Text(pojo.itemCount.toString()) },
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
