package us.huseli.thoucylinder.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.screens.LocalMusicImportUriDialog
import us.huseli.thoucylinder.viewmodels.SettingsViewModel

@Composable
fun AskMusicImportPermissions(viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val autoImportLocalMusic by viewModel.autoImportLocalMusic.collectAsStateWithLifecycle()
    val musicImportUri by viewModel.musicImportUri.collectAsStateWithLifecycle()
    var isDialogShown by rememberSaveable { mutableStateOf(autoImportLocalMusic == null) }

    if (isDialogShown) {
        LocalMusicImportUriDialog(
            currentValue = musicImportUri,
            onCancelClick = {
                viewModel.setAutoImportLocalMusic(false)
                isDialogShown = false
            },
            onSave = { uri ->
                viewModel.setAutoImportLocalMusic(true)
                viewModel.setMusicImportUri(uri)
                isDialogShown = false
                viewModel.importNewLocalAlbums(context)
            },
            cancelButtonText = stringResource(R.string.don_t_import),
            onDismissRequest = { isDialogShown = false },
        )
    }
}
