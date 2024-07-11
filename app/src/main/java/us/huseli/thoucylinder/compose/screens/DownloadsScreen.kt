package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import us.huseli.retaintheme.extensions.isoDateTime
import us.huseli.retaintheme.ui.theme.LocalBasicColors
import us.huseli.thoucylinder.DownloadTaskState
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.compose.utils.BasicHeader
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.compose.utils.ItemListCardWithThumbnail
import us.huseli.thoucylinder.dataclasses.artist.joined
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.DownloadsViewModel

@Composable
fun DownloadsScreen(viewModel: DownloadsViewModel = hiltViewModel()) {
    val trackDownloadTasks by viewModel.trackDownloadTasks.collectAsStateWithLifecycle(persistentListOf())
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Column {
        BasicHeader(title = stringResource(R.string.downloads))

        ItemList(
            things = { trackDownloadTasks },
            key = { it.track.trackId },
            isLoading = isLoading,
            onEmpty = { Text(stringResource(R.string.no_downloads_found)) },
            contentType = "TrackDownloadTask",
        ) { task ->
            val progress by task.downloadProgress.collectAsStateWithLifecycle()
            val state by task.state.collectAsStateWithLifecycle()

            ItemListCardWithThumbnail(
                thumbnailModel = task.albumArtThumbnailUrl,
                thumbnailPlaceholder = Icons.Sharp.MusicNote,
                height = 60.dp,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.started.isoDateTime(),
                        style = FistopyTheme.bodyStyles.secondarySmall,
                    )
                    Text(
                        text = task.track.title.umlautify(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (state == DownloadTaskState.ERROR) task.error?.also { error ->
                        Text(
                            text = error.toString().umlautify(),
                            maxLines = 1,
                            style = FistopyTheme.bodyStyles.primarySmall,
                            color = LocalBasicColors.current.Red,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else (task.trackArtists.joined() ?: task.albumArtists?.joined())?.also {
                        Text(
                            text = it.umlautify(),
                            maxLines = 1,
                            style = FistopyTheme.bodyStyles.primarySmall,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Box {
                    val iconModifier = Modifier.align(Alignment.Center).size(28.dp)

                    CircularProgressIndicator(
                        progress = { progress.toFloat() },
                        modifier = Modifier.padding(vertical = 10.dp).size(28.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
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
}
