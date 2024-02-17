package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material.icons.sharp.Cancel
import androidx.compose.material.icons.sharp.CheckCircle
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.ui.theme.LocalBasicColors
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify

@Composable
fun ImportableAlbumRow(
    imageBitmap: ImageBitmap?,
    isImported: Boolean,
    isNotFound: Boolean,
    albumTitle: String,
    artist: String?,
    thirdRow: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Thumbnail(
            image = imageBitmap,
            shape = MaterialTheme.shapes.extraSmall,
            placeholderIcon = when {
                isImported -> Icons.Sharp.CheckCircle
                isNotFound -> Icons.Sharp.Cancel
                else -> Icons.Sharp.Album
            },
            placeholderIconTint = when {
                isImported -> LocalBasicColors.current.Green
                isNotFound -> LocalBasicColors.current.Red
                else -> null
            },
        )

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            val textColor =
                if (isImported || isNotFound) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified

            Text(
                text = albumTitle.umlautify(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = ThouCylinderTheme.typographyExtended.listNormalHeader,
                color = textColor,
            )
            artist?.also {
                Text(
                    text = artist.umlautify(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = ThouCylinderTheme.typographyExtended.listNormalSubtitle,
                    color = textColor,
                )
            }
            if (isImported) {
                Badge(
                    containerColor = LocalBasicColors.current.Green,
                    content = { Text(text = stringResource(R.string.imported)) },
                )
            } else if (isNotFound) {
                Badge(
                    containerColor = LocalBasicColors.current.Red,
                    content = { Text(text = stringResource(R.string.no_match_found)) },
                )
            } else {
                thirdRow()
            }
        }
    }
}
