package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Cancel
import androidx.compose.material.icons.sharp.CheckCircle
import androidx.compose.material.icons.sharp.Download
import androidx.compose.material.icons.sharp.MusicNote
import androidx.compose.material.icons.sharp.Pending
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.ui.theme.LocalBasicColors
import us.huseli.thoucylinder.DownloadTaskState
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.isoDateTime
import us.huseli.thoucylinder.viewmodels.DownloadsViewModel

@Composable
fun DownloadsScreen(viewModel: DownloadsViewModel = hiltViewModel()) {
    val trackDownloadTasks by viewModel.trackDownloadTasks.collectAsStateWithLifecycle(emptyList())
    val context = LocalContext.current

    ItemList(
        things = trackDownloadTasks,
        cardHeight = 60.dp,
        gap = 5.dp,
    ) { task ->
        val thumbnail = remember { mutableStateOf<ImageBitmap?>(null) }
        val progress by task.downloadProgress.collectAsStateWithLifecycle()
        val state by task.state.collectAsStateWithLifecycle()

        LaunchedEffect(task.track) {
            thumbnail.value = viewModel.getTrackThumbnail(task.track, task.albumPojo, context)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Thumbnail(
                image = thumbnail.value,
                shape = MaterialTheme.shapes.extraSmall,
                placeholderIcon = Icons.Sharp.MusicNote,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.started.isoDateTime(),
                    style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                )
                Text(
                    text = task.track.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state == DownloadTaskState.ERROR) task.error?.also { error ->
                    Text(
                        text = error.toString(),
                        maxLines = 1,
                        style = ThouCylinderTheme.typographyExtended.listSmallTitle,
                        color = LocalBasicColors.current.Red,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else (task.track.artist ?: task.albumPojo?.album?.artist)?.also {
                    Text(
                        text = it,
                        maxLines = 1,
                        style = ThouCylinderTheme.typographyExtended.listSmallTitle,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Box {
                val iconModifier = Modifier.align(Alignment.Center).size(30.dp)

                CircularProgressIndicator(
                    progress = progress.toFloat(),
                    modifier = Modifier.padding(vertical = 10.dp).size(30.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                )

                when (state) {
                    DownloadTaskState.CANCELLED, DownloadTaskState.ERROR -> Icon(
                        imageVector = Icons.Sharp.Download,
                        contentDescription = null,
                        modifier = iconModifier.clickable { task.start() },
                        tint = MaterialTheme.colorScheme.primaryContainer,
                    )
                    DownloadTaskState.FINISHED -> Icon(
                        imageVector = Icons.Sharp.CheckCircle,
                        contentDescription = null,
                        modifier = iconModifier,
                        tint = MaterialTheme.colorScheme.primaryContainer,
                    )
                    DownloadTaskState.CREATED -> Icon(
                        imageVector = Icons.Sharp.Pending,
                        contentDescription = null,
                        modifier = iconModifier,
                        tint = MaterialTheme.colorScheme.primaryContainer,
                    )
                    else -> Icon(
                        imageVector = Icons.Sharp.Cancel,
                        contentDescription = null,
                        modifier = iconModifier.clickable { task.cancel() },
                        tint = MaterialTheme.colorScheme.primaryContainer,
                    )
                }
            }
        }
    }
}
