package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.InterpreterMode
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.ItemGrid
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.pojos.ArtistPojo
import us.huseli.thoucylinder.dataclasses.getMediaStoreFileNullable
import us.huseli.retaintheme.toBitmap
import java.io.File

@Composable
fun ArtistGrid(
    artists: List<ArtistPojo>,
    images: Map<String, File>,
    onArtistClick: (String) -> Unit,
    onEmpty: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(vertical = 10.dp),
) {
    val context = LocalContext.current

    ItemGrid(
        things = artists,
        onClick = { onArtistClick(it.name) },
        contentPadding = contentPadding,
        key = { it.name },
        onEmpty = onEmpty,
    ) { artist ->
        val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }

        LaunchedEffect(Unit) {
            imageBitmap.value = images[artist.name.lowercase()]?.toBitmap()?.asImageBitmap()
                ?: artist.firstAlbumArt?.let {
                    context.getMediaStoreFileNullable(it)?.toBitmap()?.asImageBitmap()
                }
        }

        Thumbnail(
            image = imageBitmap.value,
            modifier = Modifier.fillMaxWidth(),
            borderWidth = null,
            placeholderIcon = Icons.Sharp.InterpreterMode,
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.padding(horizontal = 5.dp, vertical = 10.dp).weight(1f)) {
                Text(
                    text = artist.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = ThouCylinderTheme.typographyExtended.listSmallHeader,
                )
                Text(
                    style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                    text = pluralStringResource(R.plurals.x_tracks, artist.trackCount, artist.trackCount),
                )
            }
        }
    }
}
