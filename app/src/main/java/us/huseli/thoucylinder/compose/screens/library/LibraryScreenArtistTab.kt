package us.huseli.thoucylinder.compose.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import us.huseli.thoucylinder.enums.ArtistSortParameter
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.ArtistGrid
import us.huseli.thoucylinder.compose.ArtistList
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListSettingsRow
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.compose.utils.CancelButton
import us.huseli.thoucylinder.compose.utils.CollapsibleToolbar
import us.huseli.thoucylinder.compose.utils.ListActions
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.ArtistListViewModel

@Composable
fun LibraryScreenArtistTab(
    displayType: DisplayType,
    isImporting: Boolean,
    onArtistClick: (String) -> Unit,
    onDisplayTypeChange: (DisplayType) -> Unit,
    onListTypeChange: (ListType) -> Unit,
    showToolbars: Boolean,
    modifier: Modifier = Modifier,
    listModifier: Modifier = Modifier,
    viewModel: ArtistListViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var isFilterDialogOpen by rememberSaveable { mutableStateOf(false) }

    val artistCombos by viewModel.artistCombos.collectAsStateWithLifecycle(persistentListOf())
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val onlyShowArtistsWithAlbums by viewModel.onlyShowArtistsWithAlbums.collectAsStateWithLifecycle()
    val searchTerm by viewModel.searchTerm.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val sortParameter by viewModel.sortParameter.collectAsStateWithLifecycle()

    val progressIndicatorText =
        if (isImporting) stringResource(R.string.importing_local_artists)
        else if (isLoading) stringResource(R.string.loading_artists)
        else null

    val onEmpty: @Composable () -> Unit = {
        if (!isImporting && !isLoading) Text(
            stringResource(R.string.no_artists_found),
            modifier = Modifier.padding(10.dp),
        )
    }

    if (isFilterDialogOpen) {
        AlertDialog(
            shape = MaterialTheme.shapes.small,
            onDismissRequest = { isFilterDialogOpen = false },
            confirmButton = {
                CancelButton(
                    onClick = { isFilterDialogOpen = false },
                    content = { Text(stringResource(R.string.close)) },
                )
            },
            title = { Text(stringResource(R.string.filters)) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.only_show_artists_with_albums),
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = onlyShowArtistsWithAlbums,
                        onCheckedChange = { viewModel.setOnlyShowArtistsWithAlbums(it) },
                    )
                }
            },
        )
    }

    CollapsibleToolbar(show = showToolbars) {
        ListSettingsRow(
            displayType = displayType,
            listType = ListType.ARTISTS,
            onDisplayTypeChange = onDisplayTypeChange,
            onListTypeChange = onListTypeChange,
            availableDisplayTypes = listOf(DisplayType.LIST, DisplayType.GRID),
        )
        ListActions(
            initialSearchTerm = searchTerm,
            sortParameter = sortParameter,
            sortOrder = sortOrder,
            sortParameters = ArtistSortParameter.withLabels(context),
            sortDialogTitle = stringResource(R.string.artist_order),
            onSort = { param, order -> viewModel.setSorting(param, order) },
            onSearch = { viewModel.setSearchTerm(it) },
            showFilterButton = false,
            extraButtons = {
                InputChip(
                    selected = onlyShowArtistsWithAlbums,
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
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        when (displayType) {
            DisplayType.LIST -> ArtistList(
                onArtistClick = onArtistClick,
                progressIndicatorText = progressIndicatorText,
                artistCombos = artistCombos,
                onEmpty = onEmpty,
                modifier = listModifier,
            )
            DisplayType.GRID -> ArtistGrid(
                onArtistClick = onArtistClick,
                artistCombos = artistCombos,
                progressIndicatorText = progressIndicatorText,
                onEmpty = onEmpty,
                modifier = listModifier,
            )
        }
    }
}
