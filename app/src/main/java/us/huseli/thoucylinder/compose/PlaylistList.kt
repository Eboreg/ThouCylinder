package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.QueueMusic
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.extensions.sensibleFormat
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistPojo
import us.huseli.thoucylinder.pluralStringResource
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify
import java.util.UUID

@Composable
fun PlaylistList(
    playlists: List<PlaylistPojo>,
    isLoading: Boolean,
    onPlaylistPlayClick: (PlaylistPojo) -> Unit,
    onPlaylistClick: (PlaylistPojo) -> Unit,
    getImage: suspend (UUID) -> ImageBitmap?,
) {
    ItemList(
        things = playlists,
        onClick = { _, pojo -> onPlaylistClick(pojo) },
        cardHeight = 60.dp,
        key = { _, pojo -> pojo.playlistId },
        onEmpty = {
            if (!isLoading) Text(
                text = stringResource(R.string.no_playlists_found),
                modifier = Modifier.padding(10.dp),
            )
        },
        progressIndicatorText = if (isLoading) stringResource(R.string.loading_playlists) else null,
    ) { _, playlist ->
        val imageBitmap = remember(playlist) { mutableStateOf<ImageBitmap?>(null) }

        LaunchedEffect(playlist) {
            getImage(playlist.playlistId)?.also { imageBitmap.value = it }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Thumbnail(
                image = imageBitmap.value,
                shape = MaterialTheme.shapes.extraSmall,
                placeholderIcon = Icons.AutoMirrored.Sharp.QueueMusic,
            )

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                val secondLine = listOf(
                    pluralStringResource(R.plurals.x_tracks, playlist.trackCount, playlist.trackCount),
                    playlist.totalDuration.sensibleFormat(),
                )

                Text(
                    text = playlist.name.umlautify(),
                    maxLines = 2,
                    style = ThouCylinderTheme.typographyExtended.listNormalHeader,
                    overflow = TextOverflow.Ellipsis,
                )
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
