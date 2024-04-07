package us.huseli.thoucylinder.compose.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.CancelButton
import us.huseli.thoucylinder.compose.utils.SaveButton
import us.huseli.thoucylinder.enums.Region
import us.huseli.thoucylinder.stringResource

@Composable
fun RegionSettingDialog(
    currentRegion: Region,
    onCancelClick: () -> Unit,
    onDismissRequest: () -> Unit = onCancelClick,
    onSave: (Region) -> Unit,
) {
    val context = LocalContext.current
    var textFieldValue by rememberSaveable { mutableStateOf("") }
    var selectedRegion by rememberSaveable(currentRegion) { mutableStateOf(currentRegion) }
    val regions by remember(textFieldValue) { mutableStateOf(Region.filteredEntries(context, textFieldValue)) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        val index = regions.indexOf(selectedRegion)
        if (index > -1) listState.scrollToItem(index, 1)
    }

    AlertDialog(
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onDismissRequest,
        dismissButton = { CancelButton(onClick = onCancelClick) },
        confirmButton = { SaveButton(onClick = { onSave(selectedRegion) }) },
        title = { Text(stringResource(R.string.select_region)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.search_for_a_region)) },
                )
                LazyColumn(state = listState) {
                    itemsIndexed(regions, key = { _, region -> region.name }) { index, region ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = stringResource(region.stringRes),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor =
                                if (region == selectedRegion) MaterialTheme.colorScheme.primaryContainer
                                else Color.Unspecified
                            ),
                            modifier = Modifier.clickable { selectedRegion = region },
                        )
                        if (index != regions.lastIndex) HorizontalDivider()
                    }
                }
            }
        },
    )
}
