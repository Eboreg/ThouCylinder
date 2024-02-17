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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.isInLandscapeMode
import us.huseli.thoucylinder.Constants.LASTFM_AUTH_URL
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.Umlautify
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.SettingsViewModel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    appCallbacks: AppCallbacks,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    val autoImportLocalMusic by viewModel.autoImportLocalMusic.collectAsStateWithLifecycle()
    val lastFmUsername by viewModel.lastFmUsername.collectAsStateWithLifecycle()
    val localMusicUri by viewModel.localMusicUri.collectAsStateWithLifecycle()
    val lastFmScrobble by viewModel.lastFmScrobble.collectAsStateWithLifecycle()
    val umlautify by viewModel.umlautify.collectAsStateWithLifecycle()

    var showLastFmUsernameDialog by rememberSaveable { mutableStateOf(false) }
    var showLocalMusicUriDialog by rememberSaveable { mutableStateOf(false) }

    if (showLocalMusicUriDialog) {
        LocalMusicUriDialog(
            currentValue = localMusicUri,
            onCancelClick = { showLocalMusicUriDialog = false },
            onSave = { uri ->
                viewModel.setLocalMusicUri(uri)
                showLocalMusicUriDialog = false
                if (autoImportLocalMusic == true) viewModel.importNewLocalAlbums(context)
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
                modifier = Modifier.padding(bottom = 5.dp, top = if (isInLandscapeMode()) 5.dp else 0.dp),
            ) {
                IconButton(
                    onClick = appCallbacks.onBackClick,
                    content = { Icon(Icons.AutoMirrored.Sharp.ArrowBack, stringResource(R.string.go_back)) },
                )
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }

        Column(modifier = Modifier.verticalScroll(state = rememberScrollState())) {
            StringSettingSection(
                title = stringResource(R.string.local_music_directory),
                description = stringResource(R.string.directory_for_music_downloads_from_youtube),
                currentValue = localMusicUri?.lastPathSegment ?: stringResource(R.string.none),
                onClick = { showLocalMusicUriDialog = true },
            )

            BooleanSettingSection(
                title = stringResource(R.string.auto_import_local_music),
                description = stringResource(R.string.this_also_requires_local_music_directory_to_be_set_see_above),
                checked = autoImportLocalMusic == true,
                onCheckedChange = {
                    viewModel.setAutoImportLocalMusic(it)
                    if (it) viewModel.importNewLocalAlbums(context)
                },
            )

            BooleanSettingSection(
                title = stringResource(R.string.scrobble_to_last_fm),
                description = stringResource(R.string.after_enabling_this_authorize_app_with_last_fm),
                checked = lastFmScrobble,
                onCheckedChange = {
                    if (it) uriHandler.openUri(LASTFM_AUTH_URL)
                    else viewModel.disableLastFmScrobble()
                },
            )

            StringSettingSection(
                title = stringResource(R.string.last_fm_username),
                description = stringResource(R.string.used_for_importing_your_top_albums),
                currentValue = lastFmUsername ?: stringResource(R.string.none),
                onClick = { showLastFmUsernameDialog = true },
            )

            BooleanSettingSection(
                title = stringResource(R.string.umlautify),
                description = Umlautify.transform(
                    stringResource(R.string.puts_metal_umlauts_above_random_letters_because_it_s_cool),
                    true,
                ),
                checked = umlautify,
                onCheckedChange = { viewModel.setUmlautify(it) },
            )
        }
    }
}


@Composable
fun StringSettingSection(
    title: String,
    description: String,
    currentValue: String,
    onClick: () -> Unit,
) {
    SettingSection(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = currentValue,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}


@Composable
fun BooleanSettingSection(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingSection {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = description, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}


@Composable
fun SettingSection(
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(vertical = 15.dp, horizontal = 10.dp)
            .let { if (onClick != null) it.clickable { onClick() } else it },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
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
fun LocalMusicUriDialog(
    currentValue: Uri? = null,
    text: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.this_is_where_downloaded_music_will_be_placed))
            Text(stringResource(R.string.if_you_have_chosen_to_auto_import_local_music))
            Text(
                stringResource(
                    R.string.current_value,
                    currentValue?.lastPathSegment ?: stringResource(R.string.none),
                )
            )
        }
    },
    title: @Composable () -> Unit = { Text(stringResource(R.string.local_music_directory)) },
    onCancelClick: () -> Unit = {},
    onSave: (Uri) -> Unit,
    cancelButtonText: String = stringResource(R.string.cancel),
    onDismissRequest: () -> Unit = onCancelClick,
) {
    val context = LocalContext.current
    val selectDirlauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
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
