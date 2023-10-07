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
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.circular
import us.huseli.thoucylinder.R

enum class DisplayType { LIST, GRID }

enum class ListType { ALBUMS, TRACKS, ARTISTS, PLAYLISTS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListSettings(
    displayType: DisplayType,
    listType: ListType,
    onDisplayTypeChange: (DisplayType) -> Unit,
    onListTypeChange: (ListType) -> Unit,
    modifier: Modifier = Modifier,
    availableDisplayTypes: List<DisplayType> = DisplayType.values().toList(),
    excludeListTypes: List<ListType> = emptyList(),
) {
    val nextDisplayType = { dt: DisplayType ->
        availableDisplayTypes.indexOf(dt).let { index ->
            if (index == availableDisplayTypes.lastIndex) availableDisplayTypes[0]
            else availableDisplayTypes[index + 1]
        }
    }

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!excludeListTypes.contains(ListType.ALBUMS)) {
                InputChip(
                    selected = listType == ListType.ALBUMS,
                    onClick = { onListTypeChange(ListType.ALBUMS) },
                    label = { Text(text = stringResource(R.string.albums)) },
                )
            }
            if (!excludeListTypes.contains(ListType.TRACKS)) {
                InputChip(
                    selected = listType == ListType.TRACKS,
                    onClick = { onListTypeChange(ListType.TRACKS) },
                    label = { Text(text = stringResource(R.string.tracks)) },
                )
            }
            if (!excludeListTypes.contains(ListType.ARTISTS)) {
                InputChip(
                    selected = listType == ListType.ARTISTS,
                    onClick = { onListTypeChange(ListType.ARTISTS) },
                    label = { Text(text = stringResource(R.string.artists)) },
                )
            }
            if (!excludeListTypes.contains(ListType.PLAYLISTS)) {
                InputChip(
                    selected = listType == ListType.PLAYLISTS,
                    onClick = { onListTypeChange(ListType.PLAYLISTS) },
                    label = { Text(text = stringResource(R.string.playlists)) },
                )
            }
        }

        IconButton(
            onClick = { onDisplayTypeChange(nextDisplayType(displayType)) },
            enabled = availableDisplayTypes.size > 1,
            content = {
                when (displayType) {
                    DisplayType.LIST -> Icon(Icons.Sharp.GridView, null)
                    DisplayType.GRID -> Icon(Icons.AutoMirrored.Sharp.ViewList, null)
                }
            }
        )
    }
}
