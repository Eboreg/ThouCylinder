package us.huseli.thoucylinder.compose.artist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.PlaylistAdd
import androidx.compose.material.icons.automirrored.sharp.PlaylistPlay
import androidx.compose.material.icons.sharp.InterpreterMode
import androidx.compose.material.icons.sharp.MoreVert
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.Radio
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.BottomSheetItem
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.artist.ArtistUiState
import us.huseli.thoucylinder.dataclasses.artist.LocalArtistCallbacks
import us.huseli.thoucylinder.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistBottomSheet(
    state: ArtistUiState,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val callbacks = LocalArtistCallbacks.current
    val uriHandler = LocalUriHandler.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .height(50.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Thumbnail(model = state.thumbnailUri, placeholderIcon = Icons.Sharp.InterpreterMode)
            Text(
                text = state.name,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        HorizontalDivider(modifier = Modifier.padding(10.dp))

        BottomSheetItem(
            text = stringResource(R.string.play_all),
            icon = Icons.Sharp.PlayArrow,
            onClick = {
                callbacks.onPlayClick(state.artistId)
                onDismissRequest()
            },
        )
        BottomSheetItem(
            text = stringResource(R.string.enqueue_all),
            icon = Icons.AutoMirrored.Sharp.PlaylistPlay,
            onClick = {
                callbacks.onEnqueueClick(state.artistId)
                onDismissRequest()
            },
        )
        BottomSheetItem(
            text = stringResource(R.string.start_radio),
            icon = Icons.Sharp.Radio,
            onClick = {
                callbacks.onStartRadioClick(state.artistId)
                onDismissRequest()
            },
        )
        BottomSheetItem(
            text = stringResource(R.string.add_all_to_playlist),
            icon = Icons.AutoMirrored.Sharp.PlaylistAdd,
            onClick = {
                callbacks.onAddToPlaylistClick(state.artistId)
                onDismissRequest()
            },
        )
        state.spotifyWebUrl?.also {
            BottomSheetItem(
                text = stringResource(R.string.browse_artist_on_spotify),
                icon = painterResource(R.drawable.spotify),
                onClick = {
                    uriHandler.openUri(it)
                    onDismissRequest()
                },
            )
        }
    }
}

@Composable
fun ArtistBottomSheetWithButton(state: ArtistUiState, modifier: Modifier = Modifier) {
    var isVisible by rememberSaveable { mutableStateOf(false) }

    IconButton(
        modifier = modifier.size(40.dp),
        onClick = { isVisible = !isVisible },
        content = {
            if (isVisible) ArtistBottomSheet(
                state = state,
                onDismissRequest = { isVisible = false },
            )
            Icon(Icons.Sharp.MoreVert, null, modifier = Modifier.size(30.dp))
        }
    )
}
