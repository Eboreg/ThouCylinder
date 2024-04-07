package us.huseli.thoucylinder.compose.screens.imports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.lastFm.LastFmTopAlbumsResponse
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.LastFmViewModel
import kotlin.math.max

@Composable
fun LastFmImport(
    showToolbars: Boolean,
    nestedScrollConnection: () -> NestedScrollConnection,
    viewModel: LastFmViewModel = hiltViewModel(),
    listState: LazyListState = rememberLazyListState(),
    onGotoSettingsClick: () -> Unit,
    onGotoLibraryClick: () -> Unit,
    onGotoAlbumClick: (String) -> Unit,
    backendSelection: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val externalAlbums by viewModel.offsetExternalAlbums.collectAsStateWithLifecycle(persistentListOf())
    val hasNext by viewModel.hasNext.collectAsStateWithLifecycle(false)
    val importedAlbumIds by viewModel.importedAlbumIds.collectAsStateWithLifecycle()
    val isAllSelected by viewModel.isAllSelected.collectAsStateWithLifecycle(false)
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val notFoundAlbumIds by viewModel.notFoundAlbumIds.collectAsStateWithLifecycle()
    val offset by viewModel.localOffset.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val searchTerm by viewModel.searchTerm.collectAsStateWithLifecycle()
    val selectedExternalAlbumIds by viewModel.selectedExternalAlbumIds.collectAsStateWithLifecycle()
    val totalAlbumCount by viewModel.totalAlbumCount.collectAsStateWithLifecycle(0)
    val username by viewModel.username.collectAsStateWithLifecycle()

    var importedAlbumStates by remember { mutableStateOf<List<Album.ViewState>?>(null) }
    var importMethodDialogOpen by rememberSaveable { mutableStateOf(false) }
    var notFoundAlbums by remember { mutableStateOf<List<LastFmTopAlbumsResponse.Album>?>(null) }
    var postImportDialogOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.setOffset(0)
    }

    if (postImportDialogOpen) {
        PostImportDialog(
            importedAlbumStates = importedAlbumStates?.toImmutableList() ?: persistentListOf(),
            notFoundAlbums = notFoundAlbums?.toImmutableList() ?: persistentListOf(),
            onGotoAlbumClick = onGotoAlbumClick,
            onGotoLibraryClick = onGotoLibraryClick,
            onDismissRequest = { postImportDialogOpen = false },
        )
    }

    if (importMethodDialogOpen) {
        ImportMethodDialog(
            title = stringResource(R.string.import_from_lastfm),
            onDismissRequest = { importMethodDialogOpen = false },
            onImportClick = { matchYoutube ->
                viewModel.importSelectedAlbums(matchYoutube = matchYoutube, context = context) { imported, notFound ->
                    importedAlbumStates = imported
                    notFoundAlbums = notFound
                    postImportDialogOpen = true
                }
                importMethodDialogOpen = false
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.import_method_description_1))
                    Text(stringResource(R.string.import_method_description_2_lastfm))
                }
            },
        )
    }

    LastFmImportHeader(
        hasPrevious = offset > 0,
        hasNext = hasNext,
        offset = offset,
        currentAlbumCount = externalAlbums.size,
        searchTerm = searchTerm,
        onPreviousClick = { viewModel.setOffset(max(offset - 50, 0)) },
        onNextClick = { viewModel.setOffset(offset + 50) },
        onSearch = { viewModel.setSearchTerm(it) },
        totalAlbumCount = totalAlbumCount,
        isAllSelected = isAllSelected,
        selectAllEnabled = externalAlbums.isNotEmpty(),
        onSelectAllClick = { viewModel.setSelectAll(it) },
        onImportClick = { importMethodDialogOpen = true },
        importButtonEnabled = progress == null && selectedExternalAlbumIds.isNotEmpty(),
        progress = progress,
        backendSelection = backendSelection,
        show = showToolbars,
    )

    if (username == null) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(10.dp).fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.you_need_to_configure_your_last_fm_username_in_the_settings),
                textAlign = TextAlign.Center,
            )
            OutlinedButton(
                onClick = onGotoSettingsClick,
                content = { Text(text = stringResource(R.string.go_to_settings)) },
                shape = MaterialTheme.shapes.extraSmall,
            )
        }
    }

    ImportItemList(
        onGotoAlbumClick = onGotoAlbumClick,
        albumThirdRow = { album ->
            album.playcount?.also { playCount ->
                Text(
                    text = stringResource(R.string.play_count, playCount),
                    style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                    maxLines = 1,
                )
            }
        },
        listState = listState,
        selectedExternalAlbumIds = selectedExternalAlbumIds,
        externalAlbums = externalAlbums,
        modifier = Modifier.nestedScroll(nestedScrollConnection()),
        importedAlbumIds = importedAlbumIds.toImmutableMap(),
        isSearching = isSearching,
        notFoundAlbumIds = notFoundAlbumIds.toImmutableList(),
        toggleSelected = { viewModel.toggleSelected(it) },
        onLongClick = { id -> viewModel.selectFromLastSelected(id, externalAlbums.map { it.id }) },
    )
}
