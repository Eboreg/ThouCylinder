package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ViewList
import androidx.compose.material.icons.sharp.GridView
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R

enum class DisplayType { LIST, GRID }

enum class ListType { ALBUMS, TRACKS, ARTISTS, PLAYLISTS }

@Composable
fun ListSettingsRow(
    displayType: DisplayType,
    listType: ListType,
    onDisplayTypeChange: (DisplayType) -> Unit,
    onListTypeChange: (ListType) -> Unit,
    modifier: Modifier = Modifier,
    availableDisplayTypes: List<DisplayType> = DisplayType.values().toList(),
    excludeListTypes: List<ListType> = emptyList(),
    tonalElevation: Dp = 2.dp,
) {
    Surface(
        color = BottomAppBarDefaults.containerColor,
        tonalElevation = tonalElevation,
    ) {
        Row(
            modifier = modifier.fillMaxWidth().padding(start = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ListTypeChips(current = listType, onChange = onListTypeChange, exclude = excludeListTypes)
            }
            ListDisplayTypeButton(
                current = displayType,
                onChange = onDisplayTypeChange,
                available = availableDisplayTypes,
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListTypeChips(
    current: ListType,
    onChange: (ListType) -> Unit,
    modifier: Modifier = Modifier,
    exclude: List<ListType> = emptyList(),
) {
    if (!exclude.contains(ListType.ALBUMS)) {
        InputChip(
            modifier = modifier,
            selected = current == ListType.ALBUMS,
            onClick = { onChange(ListType.ALBUMS) },
            label = { Text(text = stringResource(R.string.albums)) },
        )
    }
    if (!exclude.contains(ListType.TRACKS)) {
        InputChip(
            modifier = modifier,
            selected = current == ListType.TRACKS,
            onClick = { onChange(ListType.TRACKS) },
            label = { Text(text = stringResource(R.string.tracks)) },
        )
    }
    if (!exclude.contains(ListType.ARTISTS)) {
        InputChip(
            modifier = modifier,
            selected = current == ListType.ARTISTS,
            onClick = { onChange(ListType.ARTISTS) },
            label = { Text(text = stringResource(R.string.artists)) },
        )
    }
    if (!exclude.contains(ListType.PLAYLISTS)) {
        InputChip(
            modifier = modifier,
            selected = current == ListType.PLAYLISTS,
            onClick = { onChange(ListType.PLAYLISTS) },
            label = { Text(text = stringResource(R.string.playlists)) },
        )
    }
}


@Composable
fun ListDisplayTypeButton(
    current: DisplayType,
    onChange: (DisplayType) -> Unit,
    modifier: Modifier = Modifier,
    available: List<DisplayType> = DisplayType.values().toList(),
) {
    val nextDisplayType = { dt: DisplayType ->
        available.indexOf(dt).let { index ->
            if (index == available.lastIndex) available[0]
            else available[index + 1]
        }
    }

    IconButton(
        modifier = modifier,
        onClick = { onChange(nextDisplayType(current)) },
        enabled = available.size > 1,
        content = {
            when (current) {
                DisplayType.LIST -> Icon(Icons.Sharp.GridView, null)
                DisplayType.GRID -> Icon(Icons.Sharp.ViewList, null)
            }
        }
    )
}
