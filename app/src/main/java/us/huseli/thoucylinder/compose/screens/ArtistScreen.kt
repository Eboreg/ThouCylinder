package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.TrackList
import us.huseli.thoucylinder.viewmodels.ArtistViewModel
import java.util.UUID

@Composable
fun ArtistScreen(
    modifier: Modifier = Modifier,
    viewModel: ArtistViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onAlbumClick: (UUID) -> Unit,
) {
    val tracks by viewModel.tracks.collectAsStateWithLifecycle(emptyList())
    val artist = viewModel.artist

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBackClick,
                content = { Icon(Icons.AutoMirrored.Sharp.ArrowBack, stringResource(R.string.go_back)) }
            )
            Text(text = artist, style = MaterialTheme.typography.headlineSmall)
        }

        TrackList(
            tracks = tracks,
            viewModel = viewModel,
            onDownloadClick = { viewModel.downloadTrack(it) },
            onPlayOrPauseClick = { viewModel.playOrPause(it) },
            onGotoAlbumClick = onAlbumClick,
            showArtist = { track -> track.artist != artist },
        )
    }
}
