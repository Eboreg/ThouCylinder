package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.SkipNext
import androidx.compose.material.icons.sharp.SkipPrevious
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.ImportLastFm
import us.huseli.thoucylinder.compose.ImportSpotify
import us.huseli.thoucylinder.compose.utils.SmallOutlinedButton
import us.huseli.thoucylinder.dataclasses.ImportProgressData

enum class ImportBackend { SPOTIFY, LAST_FM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(modifier: Modifier = Modifier, onGotoSettingsClick: () -> Unit, onGotoLibraryClick: () -> Unit) {
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
            ImportBackend.SPOTIFY -> ImportSpotify(
                onGotoLibraryClick = onGotoLibraryClick,
                backendSelection = backendSelection,
            )
            ImportBackend.LAST_FM -> ImportLastFm(
                onGotoSettingsClick = onGotoSettingsClick,
                onGotoLibraryClick = onGotoLibraryClick,
                backendSelection = backendSelection,
            )
        }
    }
}

@Composable
fun SelectAllCheckbox(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier,
    ) {
        Text(text = stringResource(R.string.select_all))
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
fun ProgressSection(progress: ImportProgressData?) {
    if (progress != null) {
        val statusText = stringResource(progress.status.stringId)

        Column(
            modifier = Modifier.padding(top = 5.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(text = "$statusText ${progress.item} …", style = MaterialTheme.typography.labelLarge)
            LinearProgressIndicator(
                progress = progress.progress.toFloat(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun PaginationSection(
    currentAlbumCount: Int,
    offset: Int,
    totalAlbumCount: Int?,
    isTotalAlbumCountExact: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmallOutlinedButton(
            onClick = onPreviousClick,
            content = { Icon(Icons.Sharp.SkipPrevious, stringResource(R.string.previous)) },
            enabled = hasPrevious,
        )
        if (currentAlbumCount > 0) {
            val prefix = if (isTotalAlbumCountExact) "" else "≥ "
            Text(text = "${offset + 1} - ${currentAlbumCount + offset} ($prefix${totalAlbumCount ?: "?"})")
        }
        SmallOutlinedButton(
            onClick = onNextClick,
            content = { Icon(Icons.Sharp.SkipNext, stringResource(R.string.next)) },
            enabled = hasNext,
        )
    }
}
