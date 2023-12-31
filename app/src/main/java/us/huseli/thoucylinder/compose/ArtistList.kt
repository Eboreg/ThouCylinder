package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.InterpreterMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.sensibleFormat
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.getMediaStoreFileNullable
import us.huseli.thoucylinder.dataclasses.pojos.ArtistPojo
import us.huseli.retaintheme.toBitmap
import java.io.File

@Composable
fun ArtistList(
    artists: List<ArtistPojo>,
    images: Map<String, File>,
    onEmpty: (@Composable () -> Unit)? = null,
    onArtistClick: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current

    ItemList(
        things = artists,
        onClick = onArtistClick?.let { { onArtistClick(it.name) } },
        key = { it.name },
        onEmpty = onEmpty,
    ) { artist ->
        val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }

        LaunchedEffect(Unit) {
            imageBitmap.value = images[artist.name.lowercase()]?.toBitmap()?.asImageBitmap()
                ?: artist.firstAlbumArtThumbnail?.let {
                    context.getMediaStoreFileNullable(it)?.toBitmap()?.asImageBitmap()
                }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Thumbnail(
                image = imageBitmap.value,
                shape = MaterialTheme.shapes.extraSmall,
                placeholderIcon = Icons.Sharp.InterpreterMode,
            )
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = artist.name,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = ThouCylinderTheme.typographyExtended.listNormalHeader,
                )
                Text(
                    style = ThouCylinderTheme.typographyExtended.listNormalSubtitleSecondary,
                    text = pluralStringResource(R.plurals.x_albums, artist.albumCount, artist.albumCount) + " • " +
                        pluralStringResource(R.plurals.x_tracks, artist.trackCount, artist.trackCount) + " • " +
                        artist.totalDuration.sensibleFormat(),
                )
            }
        }
    }
}
