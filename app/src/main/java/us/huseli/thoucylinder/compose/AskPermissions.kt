package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import us.huseli.thoucylinder.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.screens.LocalMusicUriDialog
import us.huseli.thoucylinder.viewmodels.SettingsViewModel

@Composable
fun AskMusicImportPermissions(viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val autoImportLocalMusic by viewModel.autoImportLocalMusic.collectAsStateWithLifecycle()
    val localMusicUri by viewModel.localMusicUri.collectAsStateWithLifecycle()
    var isDialogShown by rememberSaveable(autoImportLocalMusic) { mutableStateOf(autoImportLocalMusic == null) }

    if (isDialogShown) {
        LocalMusicUriDialog(
            currentValue = localMusicUri,
            onCancelClick = {
                viewModel.setAutoImportLocalMusic(false)
                isDialogShown = false
            },
            onSave = { uri ->
                viewModel.setAutoImportLocalMusic(true)
                viewModel.setLocalMusicUri(uri)
                isDialogShown = false
                viewModel.importNewLocalAlbums(context)
            },
            cancelButtonText = stringResource(R.string.don_t_import),
            onDismissRequest = { isDialogShown = false },
            title = { Text(stringResource(R.string.auto_import_local_music)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.if_you_want_to_auto_import_local_music_select_your_local_music_directory_here))
                    Text(stringResource(R.string.you_can_always_change_this_later_in_the_settings))
                }
            },
        )
    }
}
