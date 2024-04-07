package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.CancelButton
import us.huseli.thoucylinder.enums.RadioType
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.views.RadioCombo
import us.huseli.thoucylinder.stringResource
import kotlin.math.roundToInt

@Composable
fun RadioDialog(
    activeRadio: RadioCombo?,
    activeTrackCombo: AbstractTrackCombo?,
    activeAlbumCombo: AbstractAlbumCombo?,
    activeArtist: Artist?,
    libraryRadioNovelty: Float,
    onDeactivateClick: () -> Unit,
    onStartLibraryRadioClick: () -> Unit,
    onStartArtistRadioClick: (String) -> Unit,
    onStartAlbumRadioClick: (String) -> Unit,
    onStartTrackRadioClick: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onLibraryRadioNoveltyChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val artists = setOfNotNull(
        activeArtist,
        *(activeTrackCombo?.artists ?: emptyList()).toTypedArray(),
        *(activeAlbumCombo?.artists ?: emptyList()).toTypedArray(),
    ).filter { activeRadio?.artist != it }
    val albums = setOfNotNull(activeAlbumCombo?.album, activeTrackCombo?.album)
        .filter { activeRadio?.album != it }
    var localLibraryRadioNovelty by rememberSaveable(libraryRadioNovelty) { mutableFloatStateOf(libraryRadioNovelty) }

    AlertDialog(
        modifier = modifier.padding(10.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onDismissRequest,
        dismissButton = { CancelButton(text = stringResource(R.string.close), onClick = onDismissRequest) },
        confirmButton = {},
        title = { Text(stringResource(R.string.radio)) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            ) {
                Text(stringResource(R.string.radio_dialog_help_1))
                Text(stringResource(R.string.radio_dialog_help_2))

                if (activeRadio != null) {
                    Text(stringResource(R.string.now_active_x, activeRadio.getFullTitle(context)))
                    OutlinedButton(
                        onClick = onDeactivateClick,
                        content = { Text(stringResource(R.string.deactivate_radio), textAlign = TextAlign.Center) },
                        shape = MaterialTheme.shapes.extraSmall,
                    )
                }

                for (artist in artists.take(5)) {
                    OutlinedButton(
                        onClick = { onStartArtistRadioClick(artist.artistId) },
                        content = {
                            Text(
                                stringResource(
                                    R.string.start_x_x_radio,
                                    artist.name,
                                    stringResource(R.string.artist).lowercase(),
                                ),
                                textAlign = TextAlign.Center,
                            )
                        },
                        shape = MaterialTheme.shapes.extraSmall,
                    )
                }

                for (album in albums) {
                    OutlinedButton(
                        onClick = { onStartAlbumRadioClick(album.albumId) },
                        content = {
                            Text(
                                stringResource(
                                    R.string.start_x_x_radio,
                                    album.title,
                                    stringResource(R.string.album).lowercase(),
                                ),
                                textAlign = TextAlign.Center,
                            )
                        },
                        shape = MaterialTheme.shapes.extraSmall,
                    )
                }

                if (activeTrackCombo != null) {
                    OutlinedButton(
                        onClick = { onStartTrackRadioClick(activeTrackCombo.track.trackId) },
                        content = {
                            Text(
                                stringResource(
                                    R.string.start_x_x_radio,
                                    activeTrackCombo.track.title,
                                    stringResource(R.string.track).lowercase(),
                                ),
                                textAlign = TextAlign.Center,
                            )
                        },
                        shape = MaterialTheme.shapes.extraSmall,
                    )
                }

                if (activeRadio?.type != RadioType.LIBRARY) {
                    OutlinedButton(
                        onClick = onStartLibraryRadioClick,
                        content = { Text(stringResource(R.string.start_library_radio), textAlign = TextAlign.Center) },
                        shape = MaterialTheme.shapes.extraSmall,
                    )
                    Text(stringResource(R.string.library_radio_novelty), style = MaterialTheme.typography.titleMedium)
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
                            onValueChangeFinished = { onLibraryRadioNoveltyChange(localLibraryRadioNovelty) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(
                        text = stringResource(R.string.radio_dialog_help_3),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    )
}
