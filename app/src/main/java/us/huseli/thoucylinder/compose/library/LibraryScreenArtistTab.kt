package us.huseli.thoucylinder.compose.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.artist.ArtistCollection
import us.huseli.thoucylinder.compose.utils.CancelButton
import us.huseli.thoucylinder.compose.utils.EmptyLibraryHelp
import us.huseli.thoucylinder.compose.utils.ListActions
import us.huseli.thoucylinder.enums.ArtistSortParameter
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.ArtistListViewModel

@Composable
fun LibraryScreenArtistTab(
    modifier: Modifier = Modifier,
    displayType: DisplayType = DisplayType.LIST,
    viewModel: ArtistListViewModel = hiltViewModel(),
) {
    val uiStates by viewModel.artistUiStates.collectAsStateWithLifecycle()
    val isEmpty by viewModel.isEmpty.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    ArtistCollection(
        states = { uiStates },
        displayType = displayType,
        modifier = modifier,
        isLoading = isLoading,
        onEmpty = {
            if (isEmpty) {
                Text(stringResource(R.string.no_artists_found))
                EmptyLibraryHelp()
            }
        },
    )
}

@Composable
fun ArtistListActions(viewModel: ArtistListViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var isFilterDialogOpen by rememberSaveable { mutableStateOf(false) }

    val searchTerm by viewModel.searchTerm.collectAsStateWithLifecycle()
    val showArtistsWithoutAlbums by viewModel.showArtistsWithoutAlbums.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val sortParameter by viewModel.sortParameter.collectAsStateWithLifecycle()

    ListActions(
        searchTerm = { searchTerm },
        sortParameter = sortParameter,
        sortOrder = sortOrder,
        sortParameters = ArtistSortParameter.withLabels(context),
        sortDialogTitle = stringResource(R.string.artist_order),
        onSort = remember { { param, order -> viewModel.setSorting(param, order) } },
        onSearch = remember { { viewModel.setSearchTerm(it) } },
        showFilterButton = false,
        tagPojos = { persistentListOf() },
        selectedTagPojos = { persistentListOf() },
        extraButtons = {
            InputChip(
                selected = showArtistsWithoutAlbums,
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
                        text = stringResource(R.string.show_artists_without_albums),
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = showArtistsWithoutAlbums,
                        onCheckedChange = { viewModel.setShowArtistsWithoutAlbums(it) },
                    )
                }
            },
        )
    }
}
