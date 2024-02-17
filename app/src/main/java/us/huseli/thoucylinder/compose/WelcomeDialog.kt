package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Check
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import us.huseli.thoucylinder.stringResource
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R

@Composable
fun WelcomeDialog(modifier: Modifier = Modifier, onCancel: () -> Unit) {
    AlertDialog(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onCancel,
        confirmButton = { TextButton(onClick = onCancel, content = { Text(stringResource(R.string.ok)) }) },
        title = { Text(stringResource(R.string.app_name)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(stringResource(R.string.hi_this_is_a_music_player_app))
                Text(stringResource(R.string.what_it_can_do))
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    WelcomeDialogListRow(Icons.Sharp.Check, R.string.play_local_music_files)
                    WelcomeDialogListRow(Icons.Sharp.Check, R.string.search_and_play_songs_and_albums_on_youtube)
                    WelcomeDialogListRow(Icons.Sharp.Check, R.string.download_music_from_youtube_to_local_drive)
                    WelcomeDialogListRow(
                        Icons.Sharp.Check,
                        R.string.match_your_saved_albums_on_spotify_and_play_download_them_from_youtube,
                    )
                    WelcomeDialogListRow(
                        Icons.Sharp.Check,
                        R.string.match_your_top_albums_on_last_fm_and_play_download_them_from_youtube,
                    )
                    WelcomeDialogListRow(Icons.Sharp.Check, R.string.scrobble_to_last_fm)
                }
                Text(stringResource(R.string.what_it_will_not_do))
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    WelcomeDialogListRow(Icons.Sharp.Close, R.string.identify_you_with_google)
                    WelcomeDialogListRow(Icons.Sharp.Close, R.string.cost_money)
                    WelcomeDialogListRow(Icons.Sharp.Close, R.string.display_ads)
                    WelcomeDialogListRow(Icons.Sharp.Close, R.string.harvest_your_personal_data)
                }
            }
        },
    )
}

@Composable
fun WelcomeDialogListRow(icon: ImageVector, stringRes: Int) {
    Row {
        Icon(icon, null, modifier = Modifier.padding(end = 5.dp).size(20.dp))
        Text(stringResource(stringRes))
    }
}
