package us.huseli.thoucylinder.compose.screens

import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.Constants.VALID_FILENAME_REGEX
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.viewmodels.SettingsViewModel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    appCallbacks: AppCallbacks,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val autoImportLocalMusic by viewModel.autoImportLocalMusic.collectAsStateWithLifecycle()
    val musicImportDirectory by viewModel.musicImportDirectory.collectAsStateWithLifecycle()
    val musicImportVolume by viewModel.musicImportVolume.collectAsStateWithLifecycle()
    val musicDownloadDirectory by viewModel.musicDownloadDirectory.collectAsStateWithLifecycle()
    var showMusicImportDirectoryDialog by rememberSaveable { mutableStateOf(false) }
    var showMusicDownloadDirectoryDialog by rememberSaveable { mutableStateOf(false) }
    val selectImportDirlauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val (volume, directory) = DocumentsContract.getTreeDocumentId(uri).split(':', limit = 2)
            val externalVolume =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.VOLUME_EXTERNAL_PRIMARY
                else "external"

            viewModel.setAutoImportLocalMusic(true)
            viewModel.setMusicImportVolume(if (volume == "primary") externalVolume else volume)
            viewModel.setMusicImportDirectory(directory)
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    if (showMusicDownloadDirectoryDialog) {
        MusicDownloadDirectoryDialog(
            currentValue = musicDownloadDirectory,
            onCancelClick = { showMusicDownloadDirectoryDialog = false },
            onSaveClick = {
                viewModel.setMusicDownloadDirectory(it)
                showMusicDownloadDirectoryDialog = false
            },
        )
    }

    if (showMusicImportDirectoryDialog) {
        MusicImportDirectoryDialog(
            currentValue = musicImportDirectory,
            onCancelClick = { showMusicImportDirectoryDialog = false },
            onSelectDirectoryClick = {
                val uri = Environment.getExternalStorageDirectory()
                    .toUri()
                    .buildUpon()
                    .appendPath(Environment.DIRECTORY_MUSIC)
                    .build()
                selectImportDirlauncher.launch(uri)
                showMusicImportDirectoryDialog = false
            },
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            color = BottomAppBarDefaults.containerColor,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.auto_import_local_music),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Switch(
                    checked = autoImportLocalMusic == true,
                    onCheckedChange = { viewModel.setAutoImportLocalMusic(it) },
                )
            }

            Row(modifier = Modifier.clickable { showMusicImportDirectoryDialog = true }.padding(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.music_import_directory),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.directory_from_which_to_auto_import_local_music),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = musicImportVolume?.let { "$it:$musicImportDirectory" } ?: musicImportDirectory,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Row(modifier = Modifier.clickable { showMusicDownloadDirectoryDialog = true }.padding(10.dp)) {
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
                        text = musicDownloadDirectory,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}


@Composable
fun MusicImportDirectoryDialog(
    currentValue: String,
    onCancelClick: () -> Unit,
    onSelectDirectoryClick: () -> Unit,
) {
    AlertDialog(
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onCancelClick,
        dismissButton = { TextButton(onClick = onCancelClick, content = { Text(stringResource(R.string.cancel)) }) },
        confirmButton = {
            TextButton(
                onClick = { onSelectDirectoryClick() },
                content = { Text(stringResource(R.string.select_directory)) },
            )
        },
        title = { Text(stringResource(R.string.music_import_directory)) },
        text = { Text(stringResource(R.string.current_value, currentValue)) },
    )
}


@Composable
fun MusicDownloadDirectoryDialog(
    currentValue: String,
    onCancelClick: () -> Unit,
    onSaveClick: (String) -> Unit,
) {
    var value by rememberSaveable(currentValue) {
        mutableStateOf(currentValue.substringAfter(Environment.DIRECTORY_MUSIC).trim('/'))
    }
    val isValid by rememberSaveable(value) { mutableStateOf(VALID_FILENAME_REGEX.matches(value)) }

    AlertDialog(
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onCancelClick,
        dismissButton = {
            TextButton(onClick = onCancelClick, content = { Text(text = stringResource(R.string.cancel)) })
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSaveClick(
                        if (value.trim('/').isBlank()) Environment.DIRECTORY_MUSIC
                        else "${Environment.DIRECTORY_MUSIC}/${value.trim('/')}"
                    )
                },
                content = { Text(text = stringResource(R.string.save)) },
                enabled = isValid,
            )
        },
        title = { Text(text = stringResource(R.string.music_download_directory)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = stringResource(R.string.download_directory_help_1, Environment.DIRECTORY_MUSIC))
                Text(text = stringResource(R.string.download_directory_help_2))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(text = "${Environment.DIRECTORY_MUSIC}/", modifier = Modifier.padding(top = 18.dp))
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        isError = !isValid,
                        supportingText = {
                            if (!isValid) Text(text = stringResource(R.string.contains_invalid_characters))
                        },
                        singleLine = true,
                    )
                }
            }
        }
    )
}
