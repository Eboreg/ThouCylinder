package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.InputChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.ImportLastFm
import us.huseli.thoucylinder.compose.ImportSpotify

enum class ImportBackend { SPOTIFY, LAST_FM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(modifier: Modifier = Modifier, onGotoSettingsClick: () -> Unit, onGotoLibraryClick: () -> Unit) {
    var backend by rememberSaveable { mutableStateOf(ImportBackend.SPOTIFY) }

    Column(modifier = modifier) {
        Surface(
            color = BottomAppBarDefaults.containerColor,
            tonalElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                InputChip(
                    selected = backend == ImportBackend.SPOTIFY,
                    onClick = { backend = ImportBackend.SPOTIFY },
                    label = { Text(text = stringResource(R.string.spotify)) },
                )
                InputChip(
                    selected = backend == ImportBackend.LAST_FM,
                    onClick = { backend = ImportBackend.LAST_FM },
                    label = { Text(text = stringResource(R.string.last_fm)) },
                )
            }
        }

        when (backend) {
            ImportBackend.SPOTIFY -> ImportSpotify(onGotoLibraryClick = onGotoLibraryClick)
            ImportBackend.LAST_FM -> ImportLastFm(
                onGotoSettingsClick = onGotoSettingsClick,
                onGotoLibraryClick = onGotoLibraryClick,
            )
        }
    }
}
