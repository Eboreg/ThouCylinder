package us.huseli.thoucylinder.compose.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.toImmutableList
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.screens.imports.ImportHeader
import us.huseli.thoucylinder.compose.screens.imports.ImportItemList
import us.huseli.thoucylinder.compose.screens.imports.ImportMethodDialog
import us.huseli.thoucylinder.compose.screens.imports.PostImportDialog
import us.huseli.thoucylinder.compose.utils.rememberToolbarScrollConnection
import us.huseli.thoucylinder.dataclasses.uistates.AlbumUiState
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.ExternalAlbumImportViewModel

enum class ImportBackend(@StringRes val stringRes: Int) {
    SPOTIFY(R.string.spotify),
    LAST_FM(R.string.last_fm),
    YOUTUBE(R.string.youtube),
    LOCAL(R.string.local),
}

@Composable
fun BackendSelection(
    activeBackend: ImportBackend,
    onSelect: (ImportBackend) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ImportBackend.entries.forEach { backend ->
            InputChip(
                selected = activeBackend == backend,
                onClick = { onSelect(backend) },
                label = { Text(stringResource(backend.stringRes)) },
            )
        }
    }
}

@Composable
fun ImportScreen(
    modifier: Modifier = Modifier,
    onGotoSettingsClick: () -> Unit,
    onGotoLibraryClick: () -> Unit,
    onGotoAlbumClick: (String) -> Unit,
    onGotoSearchClick: () -> Unit,
    viewModel: ExternalAlbumImportViewModel = hiltViewModel(),
    listState: LazyListState = rememberLazyListState(),
) {
    val context = LocalContext.current

    var showToolbars by remember { mutableStateOf(true) }
    val nestedScrollConnection = rememberToolbarScrollConnection { showToolbars = it }

    val backend by viewModel.currentBackend.collectAsStateWithLifecycle()
    val canImport by viewModel.canImport.collectAsStateWithLifecycle()
    val importedAlbumIds by viewModel.importedAlbumIds.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val notFoundAlbumIds by viewModel.notFoundAlbumIds.collectAsStateWithLifecycle()
    val selectedExternalAlbumIds by viewModel.selectedExternalAlbumIds.collectAsStateWithLifecycle()
    val externalAlbums by viewModel.offsetExternalAlbums.collectAsStateWithLifecycle()

    var importMethodDialogOpen by rememberSaveable { mutableStateOf(false) }
    var importedAlbumStates by remember { mutableStateOf<List<AlbumUiState>>(emptyList()) }
    var notFoundAlbums by remember { mutableStateOf<List<String>>(emptyList()) }
    var postImportDialogOpen by rememberSaveable { mutableStateOf(false) }

    if (importMethodDialogOpen) {
        ImportMethodDialog(
            title = {
                if (backend == ImportBackend.SPOTIFY) Text(stringResource(R.string.import_from_spotify))
                else if (backend == ImportBackend.LAST_FM) Text(stringResource(R.string.import_from_lastfm))
            },
            onDismissRequest = { importMethodDialogOpen = false },
            onImportClick = remember {
                { matchYoutube ->
                    viewModel.importSelectedAlbums(
                        matchYoutube = matchYoutube,
                        context = context
                    ) { imported, notFound ->
                        importedAlbumStates = imported
                        notFoundAlbums = notFound
                        postImportDialogOpen = true
                    }
                    importMethodDialogOpen = false
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (backend == ImportBackend.SPOTIFY) {
                        Text(stringResource(R.string.import_method_description_1))
                        Text(stringResource(R.string.import_method_description_2_spotify))
                    } else if (backend == ImportBackend.LAST_FM) {
                        Text(stringResource(R.string.import_method_description_1))
                        Text(stringResource(R.string.import_method_description_2_lastfm))
                    }
                }
            },
        )
    }

    if (postImportDialogOpen) {
        PostImportDialog(
            importedAlbumStates = importedAlbumStates.toImmutableList(),
            notFoundAlbums = notFoundAlbums.toImmutableList(),
            onGotoAlbumClick = {
                postImportDialogOpen = false
                onGotoAlbumClick(it)
            },
            onGotoLibraryClick = {
                postImportDialogOpen = false
                onGotoLibraryClick()
            },
            onDismissRequest = { postImportDialogOpen = false },
        )
    }

    Column(modifier = modifier) {
        ImportHeader(
            viewModel = viewModel,
            activeBackend = backend,
            canImport = canImport,
            onImportClick = {
                if (backend == ImportBackend.LOCAL || backend == ImportBackend.YOUTUBE) {
                    viewModel.importSelectedAlbums(
                        matchYoutube = false,
                        context = context,
                    ) { imported, _ ->
                        importedAlbumStates = imported
                        postImportDialogOpen = true
                    }
                } else importMethodDialogOpen = true
            },
            show = { showToolbars },
        )

        if (!canImport) {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(10.dp).fillMaxWidth(),
            ) {
                when (backend) {
                    ImportBackend.SPOTIFY -> {
                        Text(stringResource(R.string.spotify_import_help_1))
                        Text(stringResource(R.string.spotify_import_help_2))
                    }
                    ImportBackend.LAST_FM -> {
                        Text(stringResource(R.string.you_need_to_configure_your_last_fm_username_in_the_settings))
                        OutlinedButton(
                            onClick = onGotoSettingsClick,
                            content = { Text(stringResource(R.string.go_to_settings)) },
                            shape = MaterialTheme.shapes.extraSmall,
                        )
                    }
                    ImportBackend.LOCAL -> {
                        Text(stringResource(R.string.local_import_help))
                        OutlinedButton(
                            onClick = onGotoSettingsClick,
                            content = { Text(stringResource(R.string.go_to_settings)) },
                            shape = MaterialTheme.shapes.extraSmall,
                        )
                    }
                    ImportBackend.YOUTUBE -> {
                        Text(stringResource(R.string.youtube_import_help))
                        OutlinedButton(
                            onClick = onGotoSearchClick,
                            content = { Text(stringResource(R.string.go_to_search)) },
                            shape = MaterialTheme.shapes.extraSmall,
                        )
                    }
                }
            }
        }

        ImportItemList(
            albums = externalAlbums,
            importedAlbumIds = importedAlbumIds,
            isSearching = isSearching,
            onGotoAlbumClick = onGotoAlbumClick,
            toggleSelected = remember { { viewModel.toggleSelected(it) } },
            onLongClick = remember { { id -> viewModel.selectFromLastSelected(id, externalAlbums.map { it.id }) } },
            modifier = Modifier.nestedScroll(nestedScrollConnection),
            listState = listState,
            isSelected = remember(selectedExternalAlbumIds) { { selectedExternalAlbumIds.contains(it) } },
            isImported = remember(importedAlbumIds) { { importedAlbumIds.contains(it) } },
            isNotFound = remember(notFoundAlbumIds) { { notFoundAlbumIds.contains(it) } },
        )
    }
}
