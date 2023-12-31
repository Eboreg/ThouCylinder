package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.QueueMusic
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.sensibleFormat
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistPojo
import us.huseli.thoucylinder.viewmodels.LibraryViewModel

@Composable
fun PlaylistList(
    playlists: List<PlaylistPojo>,
    viewModel: LibraryViewModel,
    onPlaylistPlayClick: (PlaylistPojo) -> Unit,
    onPlaylistClick: (PlaylistPojo) -> Unit,
    onEmpty: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current

    ItemList(
        things = playlists,
        onClick = onPlaylistClick,
        cardHeight = 60.dp,
        key = { it.playlistId },
        onEmpty = onEmpty,
    ) { playlist ->
        val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }

        LaunchedEffect(Unit) {
            viewModel.getPlaylistImage(playlist.playlistId, context)?.also { imageBitmap.value = it }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Thumbnail(
                image = imageBitmap.value,
                shape = MaterialTheme.shapes.extraSmall,
                placeholderIcon = Icons.Sharp.QueueMusic,
            )

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                val secondLine = listOf(
                    pluralStringResource(R.plurals.x_tracks, playlist.trackCount, playlist.trackCount),
                    playlist.totalDuration.sensibleFormat(),
                )

                Text(text = playlist.name, maxLines = 1, style = ThouCylinderTheme.typographyExtended.listNormalHeader)
                Text(
                    text = secondLine.joinToString(" • "),
                    style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                )
            }

            if (playlist.trackCount > 0) {
                IconButton(
                    onClick = { onPlaylistPlayClick(playlist) },
                    content = { Icon(Icons.Sharp.PlayArrow, stringResource(R.string.play)) },
                )
            }
        }
    }
}
