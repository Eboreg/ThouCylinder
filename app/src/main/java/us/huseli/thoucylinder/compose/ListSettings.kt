package us.huseli.thoucylinder.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ViewList
import androidx.compose.material.icons.sharp.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.nextOrFirst
import us.huseli.thoucylinder.stringResource

enum class DisplayType { LIST, GRID }

enum class ListType(@StringRes val stringRes: Int) {
    ALBUMS(R.string.albums),
    TRACKS(R.string.tracks),
    ARTISTS(R.string.artists),
    PLAYLISTS(R.string.playlists),
}

@Composable
fun ListSettingsRow(
    currentDisplayType: DisplayType,
    currentListType: ListType,
    onDisplayTypeChange: (DisplayType) -> Unit,
    onListTypeChange: (ListType) -> Unit,
    modifier: Modifier = Modifier,
    excludeListTypes: ImmutableList<ListType> = persistentListOf(),
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            for (listType in ListType.entries) {
                if (!excludeListTypes.contains(listType)) InputChip(
                    selected = listType == currentListType,
                    onClick = { onListTypeChange(listType) },
                    label = { Text(stringResource(listType.stringRes)) },
                )
            }
        }
        ListDisplayTypeButton(
            current = currentDisplayType,
            onChange = onDisplayTypeChange,
        )
    }
}


@Composable
fun ListDisplayTypeButton(
    current: DisplayType,
    onChange: (DisplayType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nextDisplayType = remember(current) { DisplayType.entries.nextOrFirst(current) }

    IconButton(
        modifier = modifier,
        onClick = { onChange(nextDisplayType) },
        content = {
            Icon(
                imageVector = when (current) {
                    DisplayType.LIST -> Icons.Sharp.GridView
                    DisplayType.GRID -> Icons.AutoMirrored.Sharp.ViewList
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    )
}
