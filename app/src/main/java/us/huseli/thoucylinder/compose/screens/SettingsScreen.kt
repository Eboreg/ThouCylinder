package us.huseli.thoucylinder.compose.screens

import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.Constants.LASTFM_AUTH_URL
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.viewmodels.SettingsViewModel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    appCallbacks: AppCallbacks,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val autoImportLocalMusic by viewModel.autoImportLocalMusic.collectAsStateWithLifecycle()
    val lastFmUsername by viewModel.lastFmUsername.collectAsStateWithLifecycle()
    val musicDownloadUri by viewModel.musicDownloadUri.collectAsStateWithLifecycle()
    val musicImportUri by viewModel.musicImportUri.collectAsStateWithLifecycle()
    val lastFmScrobble by viewModel.lastFmScrobble.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    var showLastFmUsernameDialog by rememberSaveable { mutableStateOf(false) }
    var showMusicImportUriDialog by rememberSaveable { mutableStateOf(false) }
    var showMusicDownloadUriDialog by rememberSaveable { mutableStateOf(false) }

    if (showMusicDownloadUriDialog) {
        LocalMusicDownloadUriDialog(
            currentValue = musicDownloadUri,
            onCancelClick = { showMusicDownloadUriDialog = false },
            onSave = { uri ->
                viewModel.setMusicDownloadUri(uri)
                showMusicDownloadUriDialog = false
            },
        )
    }

    if (showMusicImportUriDialog) {
        LocalMusicImportUriDialog(
            currentValue = musicImportUri,
            onCancelClick = { showMusicImportUriDialog = false },
            onSave = { uri ->
                viewModel.setAutoImportLocalMusic(true)
                viewModel.setMusicImportUri(uri)
                showMusicImportUriDialog = false
            },
        )
    }

    if (showLastFmUsernameDialog) {
        SingleStringSettingDialog(
            currentValue = lastFmUsername,
            onCancelClick = { showLastFmUsernameDialog = false },
            onSave = { value ->
                viewModel.setLastFmUsername(value.takeIf { it.isNotEmpty() })
                showLastFmUsernameDialog = false
            },
            placeholderText = stringResource(R.string.enter_username),
            title = { Text(text = stringResource(R.string.last_fm_username)) },
            subtitle = { Text(text = stringResource(R.string.used_for_importing_your_top_albums)) },
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            color = BottomAppBarDefaults.containerColor,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.padding(bottom = 5.dp),
            ) {
                IconButton(
                    onClick = appCallbacks.onBackClick,
                    content = { Icon(Icons.Sharp.ArrowBack, stringResource(R.string.go_back)) },
                )
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }

        Column(modifier = Modifier.verticalScroll(state = rememberScrollState())) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                /*
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.auto_import_local_music),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
                 */
                Text(
                    text = stringResource(R.string.auto_import_local_music),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = autoImportLocalMusic == true,
                    onCheckedChange = { viewModel.setAutoImportLocalMusic(it) },
                )
            }

            Row(
                modifier = Modifier
                    .padding(10.dp)
                    .let { if (autoImportLocalMusic == true) it.clickable { showMusicImportUriDialog = true } else it },
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val textColor =
                        if (autoImportLocalMusic != true) MaterialTheme.colorScheme.onSurfaceVariant
                        else Color.Unspecified

                    Text(
                        text = stringResource(R.string.music_import_directory),
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor,
                    )
                    Text(
                        text = stringResource(R.string.directory_from_which_to_auto_import_local_music),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                    )
                    Text(
                        text = musicImportUri?.lastPathSegment ?: stringResource(R.string.none),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Row(modifier = Modifier.clickable { showMusicDownloadUriDialog = true }.padding(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.music_download_directory),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.directory_for_music_downloads_from_youtube),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = musicDownloadUri?.lastPathSegment ?: stringResource(R.string.none),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Row(
                modifier = Modifier.clickable { uriHandler.openUri(LASTFM_AUTH_URL) }.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.scrobble_to_last_fm),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.after_enabling_this_authorize_app_with_last_fm),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(
                    checked = lastFmScrobble,
                    onCheckedChange = {
                        viewModel.setLastFmScobble(it)
                        if (it) uriHandler.openUri(LASTFM_AUTH_URL)
                    },
                )
            }

            Row(modifier = Modifier.clickable { showLastFmUsernameDialog = true }.padding(10.dp)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.last_fm_username),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.used_for_importing_your_top_albums),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text =  lastFmUsername ?: stringResource(R.string.none),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}


@Composable
fun LocalMusicUriDialog(
    currentValue: Uri?,
    modeFlags: Int,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    cancelButtonText: String = stringResource(R.string.cancel),
    onCancelClick: () -> Unit,
    onDismissRequest: () -> Unit = onCancelClick,
    onSave: (Uri) -> Unit,
) {
    val context = LocalContext.current
    val selectDirlauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, modeFlags)
            onSave(uri)
        }
    }

    AlertDialog(
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onDismissRequest,
        dismissButton = { TextButton(onClick = onCancelClick, content = { Text(cancelButtonText) }) },
        confirmButton = {
            TextButton(
                onClick = {
                    val input = currentValue ?: Environment.getExternalStorageDirectory()
                        .toUri()
                        .buildUpon()
                        .appendPath(Environment.DIRECTORY_MUSIC)
                        .build()
                    selectDirlauncher.launch(input)
                },
                content = { Text(stringResource(R.string.select_directory)) },
            )
        },
        title = title,
        text = text,
    )
}


@Composable
fun SingleStringSettingDialog(
    currentValue: String?,
    placeholderText: String = "",
    title: @Composable () -> Unit = {},
    subtitle: @Composable () -> Unit = {},
    onCancelClick: () -> Unit,
    onDismissRequest: () -> Unit = onCancelClick,
    onSave: (String) -> Unit,
) {
    var value by rememberSaveable(currentValue) { mutableStateOf(currentValue ?: "") }

    AlertDialog(
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onDismissRequest,
        dismissButton = { TextButton(onClick = onCancelClick, content = { Text(stringResource(R.string.cancel)) }) },
        confirmButton = {
            TextButton(
                onClick = { onSave(value) },
                content = { Text(stringResource(R.string.save)) },
            )
        },
        title = title,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                subtitle()
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    placeholder = { Text(text = placeholderText) },
                )
            }
        },
    )
}


@Composable
fun LocalMusicImportUriDialog(
    currentValue: Uri?,
    cancelButtonText: String = stringResource(R.string.cancel),
    onCancelClick: () -> Unit,
    onDismissRequest: () -> Unit = onCancelClick,
    onSave: (Uri) -> Unit,
) {
    LocalMusicUriDialog(
        currentValue = currentValue,
        modeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        title = { Text(stringResource(R.string.music_import_directory)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = stringResource(R.string.select_root_directory_to_import_from))
                Text(
                    stringResource(
                        R.string.current_value,
                        currentValue?.lastPathSegment ?: stringResource(R.string.none),
                    )
                )
            }
        },
        onCancelClick = onCancelClick,
        onSave = onSave,
        cancelButtonText = cancelButtonText,
        onDismissRequest = onDismissRequest,
    )
}


@Composable
fun LocalMusicDownloadUriDialog(
    currentValue: Uri? = null,
    onCancelClick: () -> Unit = {},
    onSave: (Uri) -> Unit,
) {
    LocalMusicUriDialog(
        currentValue = currentValue,
        modeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        title = { Text(stringResource(R.string.music_download_directory)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = stringResource(R.string.download_directory_help))
                Text(
                    stringResource(
                        R.string.current_value,
                        currentValue?.lastPathSegment ?: stringResource(R.string.none),
                    )
                )
            }
        },
        onCancelClick = onCancelClick,
        onSave = onSave,
    )
}
