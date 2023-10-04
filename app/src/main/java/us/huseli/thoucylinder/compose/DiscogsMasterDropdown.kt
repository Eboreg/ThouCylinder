package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.DiscogsSearchResultItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscogsMasterDropdown(
    items: List<DiscogsSearchResultItem>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    selectedItemId: Int? = null,
    onSelect: (DiscogsSearchResultItem) -> Unit,
    onExpandedChange: ((Boolean) -> Unit)? = null,
) {
    val selectedItem = items.find { it.id == selectedItemId }
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = {
            isExpanded = it
            onExpandedChange?.invoke(it)
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        TextField(
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            value = selectedItem?.toString() ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.select_discogs_com_master)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = {
                isExpanded = false
                onExpandedChange?.invoke(false)
            },
            modifier = Modifier.exposedDropdownSize(),
        ) {
            if (isLoading) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.loading_ellipsis)) },
                    onClick = {},
                    enabled = false,
                )
            } else if (items.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.no_discogs_masters_found)) },
                    onClick = {},
                    enabled = false,
                )
            } else {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(text = item.toString()) },
                        onClick = {
                            onSelect(item)
                            isExpanded = false
                            onExpandedChange?.invoke(false)
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}
