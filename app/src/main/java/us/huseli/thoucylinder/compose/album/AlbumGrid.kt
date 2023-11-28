package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material.icons.sharp.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.ui.theme.LocalBasicColors
import us.huseli.thoucylinder.AlbumDownloadTask
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.ItemGrid
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.getDownloadProgress

@Composable
fun AlbumGrid(
    pojos: List<AbstractAlbumPojo>,
    albumCallbacks: (AbstractAlbumPojo) -> AlbumCallbacks,
    albumSelectionCallbacks: AlbumSelectionCallbacks,
    selectedAlbums: List<Album>,
    showArtist: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(vertical = 10.dp),
    albumDownloadTasks: List<AlbumDownloadTask>,
    onEmpty: @Composable (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val isSelected: (AbstractAlbumPojo) -> Boolean = { selectedAlbums.contains(it.album) }

    SelectedAlbumsButtons(albumCount = selectedAlbums.size, callbacks = albumSelectionCallbacks)

    ItemGrid(
        things = pojos,
        onClick = { pojo -> albumCallbacks(pojo).onAlbumClick?.invoke() },
        onLongClick = { pojo -> albumCallbacks(pojo).onAlbumLongClick?.invoke() },
        contentPadding = contentPadding,
        onEmpty = onEmpty,
        key = { it.album.albumId },
        isSelected = isSelected,
    ) { pojo ->
        val (downloadProgress, downloadIsActive) = getDownloadProgress(albumDownloadTasks.find { it.album.albumId == pojo.album.albumId })

        Box(modifier = Modifier.aspectRatio(1f)) {
            val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }

            LaunchedEffect(Unit) {
                imageBitmap.value = pojo.getFullImage(context)
            }

            Thumbnail(
                image = imageBitmap.value,
                borderWidth = null,
                placeholderIcon = Icons.Sharp.Album,
            )

            if (isSelected(pojo)) {
                Icon(
                    imageVector = Icons.Sharp.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    tint = LocalBasicColors.current.Green.copy(alpha = 0.7f),
                )
            }
        }

        if (downloadIsActive) {
            LinearProgressIndicator(
                progress = downloadProgress?.toFloat() ?: 0f,
                modifier = Modifier.fillMaxWidth().height(2.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = if (downloadIsActive) 3.dp else 5.dp,
                    bottom = 5.dp,
                    start = 5.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.padding(5.dp).weight(1f)) {
                val artist = pojo.album.artist?.takeIf { it.isNotBlank() && showArtist }
                val titleLines = if (artist != null) 1 else 2

                Text(
                    text = pojo.album.title,
                    maxLines = titleLines,
                    overflow = TextOverflow.Ellipsis,
                    style = ThouCylinderTheme.typographyExtended.listSmallHeader,
                )
                if (artist != null) {
                    Text(
                        text = artist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                    )
                }
            }

            AlbumContextMenuWithButton(
                isLocal = pojo.album.isLocal,
                isInLibrary = pojo.album.isInLibrary,
                isPartiallyDownloaded = pojo.isPartiallyDownloaded,
                callbacks = albumCallbacks(pojo),
            )
        }
    }
}
