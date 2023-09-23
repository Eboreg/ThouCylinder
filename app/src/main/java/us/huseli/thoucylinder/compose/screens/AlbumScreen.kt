package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowBack
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.AlbumArt
import us.huseli.thoucylinder.compose.TrackSection
import us.huseli.thoucylinder.dataclasses.YoutubePlaylist
import us.huseli.thoucylinder.viewmodels.AlbumViewModel

@Composable
fun AlbumScreen(
    modifier: Modifier = Modifier,
    viewModel: AlbumViewModel = hiltViewModel(),
    onBackClick: (() -> Unit)? = null,
    onYoutubePlaylistClick: ((YoutubePlaylist) -> Unit)? = null,
) {
    val albumNullable by viewModel.album.collectAsStateWithLifecycle()
    val albumArt by viewModel.albumArt.collectAsStateWithLifecycle()
    val albumArtLoadStatus by viewModel.albumArtLoadStatus.collectAsStateWithLifecycle()

    LazyColumn(modifier = modifier) {
        item {
            AlbumArt(image = albumArt, loadStatus = albumArtLoadStatus, modifier = Modifier.fillMaxWidth()) {
                if (onBackClick != null) {
                    FilledTonalIconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Sharp.ArrowBack, contentDescription = stringResource(R.string.go_back))
                    }
                }
            }
        }

        albumNullable?.let { album ->
            item {
                Row(
                    modifier = Modifier
                        .padding(start = 10.dp, end = if (album.youtubePlaylist != null) 0.dp else 10.dp)
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = album.toString(), modifier = Modifier.weight(1f))
                    album.youtubePlaylist?.let { playlist ->
                        IconButton(onClick = { onYoutubePlaylistClick?.invoke(playlist) }) {
                            Icon(
                                painter = painterResource(id = R.drawable.youtube),
                                contentDescription = stringResource(R.string.youtube_playlist),
                            )
                        }
                    }
                }
            }

            items(album.tracks) { track ->
                TrackSection(track = track)
            }
        }
    }
}
