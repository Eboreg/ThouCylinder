package us.huseli.thoucylinder.compose.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Forward10
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.Constants.LASTFM_AUTH_URL
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.annotatedStringResource
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.compose.utils.BasicHeader
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautStringResource
import us.huseli.thoucylinder.viewmodels.SettingsViewModel

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val uriHandler = LocalUriHandler.current

    val autoImportLocalMusic by viewModel.autoImportLocalMusic.collectAsStateWithLifecycle()
    val lastFmIsAuthenticated by viewModel.lastFmIsAuthenticated.collectAsStateWithLifecycle(false)
    val lastFmScrobble by viewModel.lastFmScrobble.collectAsStateWithLifecycle()
    val lastFmUsername by viewModel.lastFmUsername.collectAsStateWithLifecycle()
    val localMusicUri by viewModel.localMusicUri.collectAsStateWithLifecycle()
    val region by viewModel.region.collectAsStateWithLifecycle()
    val umlautify by viewModel.umlautify.collectAsStateWithLifecycle()
    val spotifyUserProfile by viewModel.spotifyUserProfile.collectAsStateWithLifecycle()

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
                if (autoImportLocalMusic == true) viewModel.importNewLocalAlbums()
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.this_is_where_downloaded_music_will_be_placed))
                    Text(stringResource(R.string.if_you_have_chosen_to_auto_import_local_music))
                    Text(
                        stringResource(
                            R.string.current_value,
                            localMusicUri?.lastPathSegment ?: stringResource(R.string.not_configured),
                        )
                    )
                }
            }
        )
    }
    Icons.Sharp.Forward10

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
            subtitle = { Text(text = annotatedStringResource(R.string.used_for_importing_your_top_albums)) },
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        BasicHeader(title = stringResource(R.string.settings))

        Column(
            modifier = Modifier.verticalScroll(state = rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            SettingsSection(title = stringResource(R.string.local_music)) {
                StringSetting(
                    title = stringResource(R.string.local_music_directory),
                    description = stringResource(R.string.directory_for_music_downloads_from_youtube),
                    currentValue = localMusicUri?.lastPathSegment ?: stringResource(R.string.not_configured),
                    onClick = { showLocalMusicUriDialog = true },
                )

                BooleanSetting(
                    title = stringResource(R.string.auto_import_local_music),
                    description = stringResource(R.string.auto_import_local_music_description),
                    checked = localMusicUri != null && autoImportLocalMusic == true,
                    onCheckedChange = {
                        viewModel.setAutoImportLocalMusic(it)
                        if (it) viewModel.importNewLocalAlbums()
                    },
                    enabled = localMusicUri != null,
                )

                StringSetting(
                    title = stringResource(R.string.unhide_all_local_albums),
                    description = stringResource(R.string.unhide_all_local_albums_description),
                    currentValue = null,
                    onClick = { viewModel.unhideLocalAlbums() },
                )
            }

            HorizontalDivider()

            SettingsSection(title = stringResource(R.string.last_fm)) {
                BooleanSetting(
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

                StringSetting(
                    title = stringResource(R.string.last_fm_username),
                    description = {
                        Text(
                            text = annotatedStringResource(R.string.used_for_importing_your_top_albums),
                            style = FistopyTheme.typography.bodyMedium,
                        )
                    },
                    currentValue = lastFmUsername ?: stringResource(R.string.not_configured),
                    onClick = { showLastFmUsernameDialog = true },
                )
            }

            HorizontalDivider()

            SettingsSection(title = stringResource(R.string.other_stuff)) {
                StringSetting(
                    title = stringResource(R.string.region),
                    description = stringResource(R.string.used_for_filtering_available_youtube_videos),
                    currentValue = stringResource(region.stringRes),
                    onClick = { showRegionDialog = true },
                )

                spotifyUserProfile?.also {
                    StringSetting(
                        title = stringResource(R.string.forget_spotify_profile),
                        description = stringResource(R.string.forget_spotify_profile_description),
                        currentValue = it.displayName ?: it.id,
                        onClick = { viewModel.forgetSpotifyProfile() },
                    )
                }

                BooleanSetting(
                    title = umlautStringResource(R.string.umlautify),
                    description = umlautStringResource(R.string.puts_metal_umlauts_above_random_letters_because_its_cool),
                    checked = umlautify,
                    onCheckedChange = { viewModel.setUmlautify(it) },
                )
            }
        }
    }
}

@Composable
inline fun SettingsSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier.padding(vertical = 10.dp)) {
        Text(
            text = title,
            style = FistopyTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            content = content,
        )
    }
}
