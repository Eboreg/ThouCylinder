package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.HiltViewModelFactory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import us.huseli.retaintheme.sensibleFormat
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.Track
import us.huseli.thoucylinder.viewmodels.TrackViewModel

@Composable
fun TrackSection(track: Track, modifier: Modifier = Modifier) {
    val owner = checkNotNull(LocalViewModelStoreOwner.current)
    val factory = (owner as? NavBackStackEntry)?.let {
        HiltViewModelFactory(context = LocalContext.current, navBackStackEntry = it)
    }
    val viewModel = viewModel<TrackViewModel>(owner, factory = factory, key = track.id.toString())

    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle(initialValue = false)

    viewModel.setTrack(track)

    ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(start = 10.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.albumPosition?.plus(1)?.let { "$it. ${track.title}" } ?: track.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(text = track.metadata.audioFormat)
            }
            Text(text = track.metadata.duration.sensibleFormat())
            IconButton(
                onClick = { viewModel.playOrPause() },
                content = {
                    if (isPlaying) {
                        Icon(
                            imageVector = Icons.Sharp.Pause,
                            contentDescription = stringResource(R.string.pause),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Sharp.PlayArrow,
                            contentDescription = stringResource(R.string.play),
                        )
                    }
                }
            )
        }
    }
}
