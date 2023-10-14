package us.huseli.thoucylinder.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Bookmark
import androidx.compose.material.icons.sharp.BookmarkBorder
import androidx.compose.material.icons.sharp.Cancel
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.sharp.Download
import androidx.compose.material.icons.sharp.Edit
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.media3.exoplayer.offline.DownloadProgress
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo

@Composable
fun AlbumButtons(
    pojo: AbstractAlbumPojo,
    onCancelDownloadClick: () -> Unit,
    onRemoveFromLibraryClick: () -> Unit,
    onAddToLibraryClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
    downloadProgress: DownloadProgress? = null,
) {
    if (!pojo.album.isLocal) {
        if (downloadProgress != null) {
            IconButton(
                onClick = onCancelDownloadClick,
                content = { Icon(Icons.Sharp.Cancel, stringResource(R.string.cancel_download)) },
                modifier = modifier,
            )
        } else {
            if (pojo.album.isInLibrary) {
                IconButton(
                    onClick = onRemoveFromLibraryClick,
                    content = {
                        Icon(Icons.Sharp.Bookmark, stringResource(R.string.remove_from_library))
                    },
                    modifier = modifier,
                )
            } else {
                IconButton(
                    onClick = onAddToLibraryClick,
                    content = {
                        Icon(Icons.Sharp.BookmarkBorder, stringResource(R.string.add_to_library))
                    },
                    modifier = modifier,
                )
            }
            IconButton(
                onClick = onDownloadClick,
                content = { Icon(Icons.Sharp.Download, stringResource(R.string.download)) },
                modifier = modifier,
            )
        }
    }
    if (pojo.album.isInLibrary) {
        IconButton(
            onClick = onEditClick,
            content = { Icon(Icons.Sharp.Edit, stringResource(R.string.edit)) },
            modifier = modifier,
        )
    }
    if (BuildConfig.DEBUG) {
        IconButton(
            onClick = onDeleteClick,
            content = { Icon(Icons.Sharp.Delete, null) },
            modifier = modifier,
        )
    }
    IconButton(
        onClick = onPlayClick,
        content = { Icon(Icons.Sharp.PlayArrow, null) },
        modifier = modifier,
    )
}
