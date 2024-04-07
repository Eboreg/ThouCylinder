package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.InputChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.screens.imports.LastFmImport
import us.huseli.thoucylinder.compose.screens.imports.SpotifyImport
import us.huseli.thoucylinder.compose.utils.rememberToolbarScrollConnection
import us.huseli.thoucylinder.stringResource

enum class ImportBackend { SPOTIFY, LAST_FM }

@Composable
fun ImportScreen(
    modifier: Modifier = Modifier,
    onGotoSettingsClick: () -> Unit,
    onGotoLibraryClick: () -> Unit,
    onGotoAlbumClick: (String) -> Unit,
) {
    var showToolbars by remember { mutableStateOf(true) }
    val nestedScrollConnection = rememberToolbarScrollConnection { showToolbars = it }
    var backend by rememberSaveable { mutableStateOf(ImportBackend.SPOTIFY) }
    val backendSelection = @Composable {
        Row(
            modifier = modifier,
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

    Column(modifier = modifier) {
        when (backend) {
            ImportBackend.SPOTIFY -> SpotifyImport(
                onGotoLibraryClick = onGotoLibraryClick,
                onGotoAlbumClick = onGotoAlbumClick,
                backendSelection = backendSelection,
                nestedScrollConnection = { nestedScrollConnection },
                showToolbars = showToolbars,
            )
            ImportBackend.LAST_FM -> LastFmImport(
                onGotoSettingsClick = onGotoSettingsClick,
                onGotoLibraryClick = onGotoLibraryClick,
                onGotoAlbumClick = onGotoAlbumClick,
                backendSelection = backendSelection,
                nestedScrollConnection = { nestedScrollConnection },
                showToolbars = showToolbars,
            )
        }
    }
}
