package us.huseli.thoucylinder.compose.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.sharp.Download
import androidx.compose.material.icons.sharp.Edit
import androidx.compose.material.icons.sharp.InterpreterMode
import androidx.compose.material.icons.sharp.MoreVert
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.BottomSheetItem
import us.huseli.thoucylinder.compose.utils.Thumbnail4x4
import us.huseli.thoucylinder.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistBottomSheet(
    name: String,
    thumbnailUris: List<String>,
    onDismissRequest: () -> Unit,
    onPlayClick: () -> Unit,
    onRenameClick: () -> Unit,
    onExportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .height(50.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Thumbnail4x4(models = thumbnailUris, placeholderIcon = Icons.Sharp.InterpreterMode)
            Text(
                text = name,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        HorizontalDivider(modifier = Modifier.padding(10.dp))

        BottomSheetItem(
            text = stringResource(R.string.play),
            icon = Icons.Sharp.PlayArrow,
            onClick = {
                onPlayClick()
                onDismissRequest()
            },
        )
        BottomSheetItem(
            text = stringResource(R.string.rename),
            icon = Icons.Sharp.Edit,
            onClick = {
                onRenameClick()
                onDismissRequest()
            },
        )
        BottomSheetItem(
            text = stringResource(R.string.export_to_playlist_file),
            icon = Icons.Sharp.Download,
            onClick = {
                onExportClick()
                onDismissRequest()
            },
        )
        BottomSheetItem(
            text = stringResource(R.string.delete),
            icon = Icons.Sharp.Delete,
            onClick = {
                onDeleteClick()
                onDismissRequest()
            },
        )
    }
}

@Composable
fun PlaylistBottomSheetWithButton(
    name: String,
    thumbnailUris: List<String>,
    onPlayClick: () -> Unit,
    onExportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRename: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isVisible by rememberSaveable { mutableStateOf(false) }
    var isRenameDialogOpen by rememberSaveable { mutableStateOf(false) }

    if (isRenameDialogOpen) {
        RenamePlaylistDialog(
            initialName = name,
            onSave = {
                onRename(it)
                isRenameDialogOpen = false
            },
            onCancel = { isRenameDialogOpen = false },
        )
    }

    IconButton(
        onClick = { isVisible = !isVisible },
        modifier = modifier,
        content = {
            Icon(Icons.Sharp.MoreVert, null)
            if (isVisible) PlaylistBottomSheet(
                name = name,
                thumbnailUris = thumbnailUris,
                onDismissRequest = { isVisible = false },
                onPlayClick = { onPlayClick() },
                onRenameClick = { isRenameDialogOpen = true },
                onExportClick = { onExportClick() },
                onDeleteClick = { onDeleteClick() },
            )
        }
    )
}
