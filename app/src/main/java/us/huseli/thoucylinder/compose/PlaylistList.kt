package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Add
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.sensibleFormat
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.PlaylistPojo

@Composable
fun PlaylistList(
    playlists: List<PlaylistPojo>,
    onPlaylistPlayClick: (PlaylistPojo) -> Unit,
    onPlaylistClick: (PlaylistPojo) -> Unit,
    onAddPlaylist: (Playlist) -> Unit,
) {
    var isAddDialogOpen by rememberSaveable { mutableStateOf(false) }

    if (isAddDialogOpen) {
        AddPlaylistDialog(
            onSave = { name ->
                onAddPlaylist(Playlist(name = name))
                isAddDialogOpen = false
            },
            onCancel = { isAddDialogOpen = false },
        )
    }

    Row(
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp, horizontal = 10.dp),
    ) {
        FilledTonalIconButton(
            onClick = { isAddDialogOpen = true },
            content = { Icon(Icons.Sharp.Add, stringResource(R.string.add_playlist)) },
        )
    }

    ItemList(things = playlists, onCardClick = onPlaylistClick, cardHeight = 60.dp) { playlist ->
        Row(
            modifier = Modifier.padding(vertical = 5.dp).fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                val secondLine = listOf(
                    pluralStringResource(R.plurals.x_tracks, playlist.trackCount, playlist.trackCount),
                    playlist.totalDuration.sensibleFormat(),
                )

                Text(text = playlist.name, maxLines = 1)
                Text(text = secondLine.joinToString(" • "), style = MaterialTheme.typography.bodySmall)
            }
            IconButton(
                onClick = { onPlaylistPlayClick(playlist) },
                content = { Icon(Icons.Sharp.PlayArrow, stringResource(R.string.play)) },
            )
        }
    }
}
