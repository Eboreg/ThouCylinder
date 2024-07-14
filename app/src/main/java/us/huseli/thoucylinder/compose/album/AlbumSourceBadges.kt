package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.compose.utils.LargeIconBadge
import us.huseli.thoucylinder.dataclasses.album.AlbumUiState
import us.huseli.thoucylinder.stringResource

@Composable
fun AlbumSourceBadges(state: AlbumUiState) {
    val uriHandler = LocalUriHandler.current
    val textStyle = FistopyTheme.typography.bodySmall

    if (state.youtubeWebUrl != null || state.spotifyWebUrl != null || state.isLocal) {
        Row(
            modifier = Modifier.padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.youtubeWebUrl?.also { youtubeUrl ->
                LargeIconBadge(modifier = Modifier.clickable { uriHandler.openUri(youtubeUrl) }) {
                    Icon(
                        painterResource(R.drawable.youtube),
                        null,
                        modifier = Modifier.height(15.dp)
                    )
                    Text(text = stringResource(R.string.youtube), style = textStyle)
                }
            }
            state.spotifyWebUrl?.also { spotifyUrl ->
                LargeIconBadge(modifier = Modifier.clickable { uriHandler.openUri(spotifyUrl) }) {
                    Icon(
                        painterResource(R.drawable.spotify),
                        null,
                        modifier = Modifier.height(12.dp)
                    )
                    Text(text = stringResource(R.string.spotify), style = textStyle)
                }
            }
            if (state.isLocal) {
                LargeIconBadge {
                    Icon(
                        painterResource(R.drawable.hard_drive_filled),
                        null,
                        Modifier.height(15.dp)
                    )
                    Text(text = stringResource(R.string.local), style = textStyle)
                }
            }
        }
    }
}
