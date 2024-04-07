package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.isInLandscapeMode
import us.huseli.thoucylinder.Constants.LASTFM_AUTH_URL
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.screens.settings.BooleanSettingSection
import us.huseli.thoucylinder.compose.screens.settings.LocalMusicUriDialog
import us.huseli.thoucylinder.compose.screens.settings.RegionSettingDialog
import us.huseli.thoucylinder.compose.screens.settings.SingleStringSettingDialog
import us.huseli.thoucylinder.compose.screens.settings.StringSettingSection
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautStringResource
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
    val lastFmIsAuthenticated by viewModel.lastFmIsAuthenticated.collectAsStateWithLifecycle(false)
    val lastFmScrobble by viewModel.lastFmScrobble.collectAsStateWithLifecycle()
    val lastFmUsername by viewModel.lastFmUsername.collectAsStateWithLifecycle()
    val localMusicUri by viewModel.localMusicUri.collectAsStateWithLifecycle()
    val region by viewModel.region.collectAsStateWithLifecycle()
    val umlautify by viewModel.umlautify.collectAsStateWithLifecycle()

    var showLastFmUsernameDialog by rememberSaveable { mutableStateOf(false) }
    var showLocalMusicUriDialog by rememberSaveable { mutableStateOf(false) }
    var showRegionDialog by rememberSaveable { mutableStateOf(false) }

    if (showRegionDialog) {
        RegionSettingDialog(
            currentRegion = region,
            onCancelClick = { showRegionDialog = false },
            onSave = {
                showRegionDialog = false
                viewModel.setRegion(it)
            },
        )
    }

    if (showLocalMusicUriDialog) {
        LocalMusicUriDialog(
            currentValue = localMusicUri,
            onCancelClick = { showLocalMusicUriDialog = false },
            onSave = { uri ->
                viewModel.setLocalMusicUri(uri)
                showLocalMusicUriDialog = false
                if (autoImportLocalMusic == true) viewModel.importNewLocalAlbumsAsync(context)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.this_is_where_downloaded_music_will_be_placed))
                    Text(stringResource(R.string.if_you_have_chosen_to_auto_import_local_music))
                    Text(
                        stringResource(
                            R.string.current_value,
                            localMusicUri?.lastPathSegment ?: stringResource(R.string.none),
                        )
                    )
                }
            }
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
                description = stringResource(R.string.this_also_requires_local_music_directory_to_be_set),
                checked = autoImportLocalMusic == true,
                onCheckedChange = {
                    viewModel.setAutoImportLocalMusic(it)
                    if (it) viewModel.importNewLocalAlbumsAsync(context)
                },
            )

            BooleanSettingSection(
                title = stringResource(R.string.scrobble_to_last_fm),
                description = stringResource(R.string.after_enabling_this_authorize_app_with_last_fm),
                checked = lastFmScrobble,
                onCheckedChange = {
                    if (it) {
                        if (!lastFmIsAuthenticated) uriHandler.openUri(LASTFM_AUTH_URL)
                        else viewModel.enableLastFmScrobble()
                    } else viewModel.disableLastFmScrobble()
                },
            )

            StringSettingSection(
                title = stringResource(R.string.last_fm_username),
                description = stringResource(R.string.used_for_importing_your_top_albums),
                currentValue = lastFmUsername ?: stringResource(R.string.none),
                onClick = { showLastFmUsernameDialog = true },
            )

            StringSettingSection(
                title = stringResource(R.string.region),
                description = stringResource(R.string.used_for_filtering_available_youtube_videos),
                currentValue = stringResource(region.stringRes),
                onClick = { showRegionDialog = true },
            )

            BooleanSettingSection(
                title = umlautStringResource(R.string.umlautify),
                description = umlautStringResource(R.string.puts_metal_umlauts_above_random_letters_because_its_cool),
                checked = umlautify,
                onCheckedChange = { viewModel.setUmlautify(it) },
            )
        }
    }
}
