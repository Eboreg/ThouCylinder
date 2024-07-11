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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.ui.theme.LocalBasicColors
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.SaveButton
import us.huseli.thoucylinder.stringResource

@Composable
fun WelcomeDialog(modifier: Modifier = Modifier, onCancel: () -> Unit) {
    AlertDialog(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onCancel,
        confirmButton = { SaveButton(onClick = onCancel, content = { Text(stringResource(R.string.ok)) }) },
        title = { Text(stringResource(R.string.app_name)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(stringResource(R.string.hi_this_is_a_music_player_app))
                Text(stringResource(R.string.what_it_can_do))
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    WelcomeDialogListRowPositive(R.string.play_local_music_files)
                    WelcomeDialogListRowPositive(R.string.search_and_play_songs_and_albums_on_youtube)
                    WelcomeDialogListRowPositive(R.string.match_your_albums_and_play_download)
                    WelcomeDialogListRowPositive(R.string.scrobble_to_last_fm)
                }
                Text(stringResource(R.string.what_it_will_not_do))
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    WelcomeDialogListRowNegative(R.string.identify_you_with_google)
                    WelcomeDialogListRowNegative(R.string.cost_money)
                    WelcomeDialogListRowNegative(R.string.display_ads)
                    WelcomeDialogListRowNegative(R.string.harvest_your_personal_data)
                }
            }
        },
    )
}

@Composable
fun WelcomeDialogListRow(icon: ImageVector, stringRes: Int, iconColor: Color = LocalContentColor.current) {
    Row {
        Icon(icon, null, modifier = Modifier.padding(end = 5.dp).size(20.dp), tint = iconColor)
        Text(stringResource(stringRes))
    }
}

@Composable
fun WelcomeDialogListRowPositive(stringRes: Int) {
    WelcomeDialogListRow(icon = Icons.Sharp.Check, stringRes = stringRes, iconColor = LocalBasicColors.current.Green)
}

@Composable
fun WelcomeDialogListRowNegative(stringRes: Int) {
    WelcomeDialogListRow(icon = Icons.Sharp.Close, stringRes = stringRes, iconColor = LocalBasicColors.current.Red)
}
