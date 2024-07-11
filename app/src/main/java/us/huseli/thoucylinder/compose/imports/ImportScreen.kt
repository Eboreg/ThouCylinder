package us.huseli.thoucylinder.compose.imports

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.isInLandscapeMode
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.annotatedStringResource
import us.huseli.thoucylinder.compose.scrollbar.ScrollbarListState
import us.huseli.thoucylinder.compose.scrollbar.rememberScrollbarListState
import us.huseli.thoucylinder.compose.utils.rememberToolbarScrollConnection
import us.huseli.thoucylinder.dataclasses.album.LocalAlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.LocalAppCallbacks
import us.huseli.thoucylinder.externalcontent.ImportBackend
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.ExternalAlbumImportViewModel

@Composable
fun ImportScreen(
    modifier: Modifier = Modifier,
    viewModel: ExternalAlbumImportViewModel = hiltViewModel(),
    scrollbarState: ScrollbarListState = rememberScrollbarListState(),
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val albumCallbacks = LocalAlbumCallbacks.current
    val appCallbacks = LocalAppCallbacks.current

    var showToolbars by remember { mutableStateOf(true) }
    val nestedScrollConnection = rememberToolbarScrollConnection { showToolbars = it }
    var importMethodDialogOpen by rememberSaveable { mutableStateOf(false) }

    val albumUiStates by viewModel.albumUiStates.collectAsStateWithLifecycle()
    val backend by viewModel.backendKey.collectAsStateWithLifecycle()
    val canImport by viewModel.canImport.collectAsStateWithLifecycle()
    val isEmpty by viewModel.isEmpty.collectAsStateWithLifecycle()
    val isLoadingCurrentPage by viewModel.isLoadingCurrentPage.collectAsStateWithLifecycle()

    val selectDirlauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.setLocalImportUri(uri)
        }
    }

    if (importMethodDialogOpen) {
        ImportMethodDialog(
            title = {
                if (backend == ImportBackend.SPOTIFY) Text(stringResource(R.string.import_from_spotify))
                else if (backend == ImportBackend.LAST_FM) Text(stringResource(R.string.import_from_lastfm))
            },
            onDismissRequest = { importMethodDialogOpen = false },
            onImportClick = remember {
                { matchYoutube ->
                    viewModel.importSelectedAlbums(matchYoutube = matchYoutube)
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

    Column(modifier = modifier) {
        ImportHeader(
            modifier = Modifier.padding(top = if (isInLandscapeMode()) 10.dp else 0.dp),
            viewModel = viewModel,
            activeBackend = backend,
            canImport = canImport,
            onImportClick = {
                if (backend == ImportBackend.LOCAL) {
                    viewModel.importSelectedAlbums(matchYoutube = false)
                } else importMethodDialogOpen = true
            },
            show = { showToolbars },
            onSelectDirectoryClick = { selectDirlauncher.launch(null) },
        )

        if (!canImport) {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(10.dp).fillMaxWidth(),
            ) {
                when (backend) {
                    ImportBackend.SPOTIFY -> {
                        Text(annotatedStringResource(R.string.spotify_import_help_1))
                        Text(stringResource(R.string.spotify_import_help_2))
                        Button(
                            onClick = { uriHandler.openUri(viewModel.getSpotifyAuthUrl()) },
                            shape = MaterialTheme.shapes.small,
                            content = { Text(stringResource(R.string.authorize)) },
                        )
                    }
                    ImportBackend.LAST_FM -> {
                        Text(stringResource(R.string.you_need_to_configure_your_last_fm_username_in_the_settings))
                        Button(
                            onClick = appCallbacks.onGotoSettingsClick,
                            content = { Text(stringResource(R.string.go_to_settings)) },
                            shape = MaterialTheme.shapes.small,
                        )
                    }
                    ImportBackend.LOCAL -> {
                        Text(stringResource(R.string.local_import_help_1))
                        Text(stringResource(R.string.local_import_help_2))
                        Button(
                            onClick = { selectDirlauncher.launch(null) },
                            content = { Text(stringResource(R.string.select_directory)) },
                            shape = MaterialTheme.shapes.small,
                        )
                        OutlinedButton(
                            onClick = appCallbacks.onGotoSettingsClick,
                            content = { Text(stringResource(R.string.go_to_settings)) },
                            shape = MaterialTheme.shapes.small,
                        )
                    }
                }
            }
        }

        ImportableAlbumList(
            uiStates = albumUiStates,
            isLoadingCurrentPage = isLoadingCurrentPage,
            isEmpty = isEmpty,
            onGotoAlbumClick = albumCallbacks.onGotoAlbumClick,
            toggleSelected = { viewModel.toggleSelected(it) },
            onLongClick = { viewModel.onAlbumLongClick(it) },
            modifier = Modifier.nestedScroll(nestedScrollConnection),
            scrollbarState = scrollbarState,
        )
    }
}
