package us.huseli.thoucylinder.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.Help
import androidx.compose.material.icons.automirrored.sharp.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.CancelButton
import us.huseli.thoucylinder.dataclasses.artist.ISavedArtist
import us.huseli.thoucylinder.enums.RadioType
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.RadioViewModel
import kotlin.math.roundToInt

@Composable
fun RadioDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RadioViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.radioDialogUiState.collectAsStateWithLifecycle()
    var localLibraryRadioNovelty by rememberSaveable(state.libraryRadioNovelty) {
        mutableFloatStateOf(state.libraryRadioNovelty)
    }
    var showHelp by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        modifier = modifier.padding(10.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onDismissRequest,
        dismissButton = { CancelButton(text = stringResource(R.string.close), onClick = onDismissRequest) },
        confirmButton = {},
        title = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.radio))
                IconButton(
                    onClick = { showHelp = !showHelp },
                    content = {
                        Icon(
                            if (showHelp) Icons.AutoMirrored.Sharp.Help else Icons.AutoMirrored.Sharp.HelpOutline,
                            null,
                        )
                    }
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            ) {
                AnimatedVisibility(visible = showHelp) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.radio_dialog_help_1))
                        Text(stringResource(R.string.radio_dialog_help_2))
                    }
                }

                state.activeRadio?.also { activeRadio ->
                    Text(
                        text = stringResource(R.string.now_active_x, activeRadio.getFullTitle(context)),
                        style = FistopyTheme.typography.titleMedium,
                    )
                    OutlinedButton(
                        onClick = { viewModel.deactivate() },
                        content = { Text(stringResource(R.string.deactivate_radio)) },
                        shape = MaterialTheme.shapes.small,
                    )
                }

                for (artist in state.artists.take(5)) {
                    val artistId = if (artist is ISavedArtist) artist.artistId else null

                    if (artistId != null) OutlinedButton(
                        onClick = { viewModel.startArtistRadio(artistId) },
                        content = {
                            Text(
                                stringResource(
                                    R.string.start_x_x_radio,
                                    artist.name,
                                    stringResource(R.string.artist).lowercase(),
                                ),
                            )
                        },
                        shape = MaterialTheme.shapes.small,
                    )
                }

                for (album in state.albums) {
                    OutlinedButton(
                        onClick = { viewModel.startAlbumRadio(album.albumId) },
                        content = {
                            Text(
                                stringResource(
                                    R.string.start_x_x_radio,
                                    album.title,
                                    stringResource(R.string.album).lowercase(),
                                ),
                            )
                        },
                        shape = MaterialTheme.shapes.small,
                    )
                }

                state.activeTrack?.also { activeTrack ->
                    OutlinedButton(
                        onClick = { viewModel.startTrackRadio(activeTrack.trackId) },
                        content = {
                            Text(
                                stringResource(
                                    R.string.start_x_x_radio,
                                    activeTrack.title,
                                    stringResource(R.string.track).lowercase(),
                                ),
                            )
                        },
                        shape = MaterialTheme.shapes.small,
                    )
                }

                if (state.activeRadio?.type != RadioType.LIBRARY) {
                    OutlinedButton(
                        onClick = { viewModel.startLibraryRadio() },
                        content = { Text(stringResource(R.string.start_library_radio)) },
                        shape = MaterialTheme.shapes.small,
                    )
                    Text(
                        text = stringResource(R.string.library_radio_novelty),
                        style = FistopyTheme.typography.titleMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = localLibraryRadioNovelty.times(100).roundToInt().toString() + "%",
                            modifier = Modifier.width(40.dp),
                        )
                        Slider(
                            value = localLibraryRadioNovelty,
                            onValueChange = { localLibraryRadioNovelty = it },
                            onValueChangeFinished = { viewModel.setLibraryRadioNovelty(localLibraryRadioNovelty) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(
                        text = stringResource(R.string.radio_dialog_help_3),
                        style = FistopyTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    )
}
