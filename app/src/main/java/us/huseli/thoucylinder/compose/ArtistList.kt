package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.InterpreterMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.views.ArtistCombo
import us.huseli.thoucylinder.pluralStringResource
import us.huseli.thoucylinder.umlautify
import java.util.UUID

@Composable
fun ArtistList(
    artistCombos: List<ArtistCombo>,
    progressIndicatorText: String? = null,
    onArtistClick: (UUID) -> Unit,
    onEmpty: @Composable (() -> Unit)? = null,
    getImage: suspend (ArtistCombo) -> ImageBitmap?,
) {
    ItemList(
        things = artistCombos,
        progressIndicatorText = progressIndicatorText,
        onClick = { _, combo -> onArtistClick(combo.artist.id) },
        onEmpty = onEmpty,
    ) { _, combo ->
        var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

        LaunchedEffect(combo.artist) {
            imageBitmap = getImage(combo)
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Thumbnail(
                image = imageBitmap,
                shape = MaterialTheme.shapes.extraSmall,
                placeholderIcon = Icons.Sharp.InterpreterMode,
            )
            Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = combo.artist.name.umlautify(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = ThouCylinderTheme.typographyExtended.listNormalHeader,
                )
                Text(
                    style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                    text = pluralStringResource(R.plurals.x_albums, combo.albumCount, combo.albumCount) + " • " +
                        pluralStringResource(R.plurals.x_tracks, combo.trackCount, combo.trackCount),
                )
            }
        }
    }
}
