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
import us.huseli.thoucylinder.compose.utils.ItemGrid
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.combos.ArtistCombo
import us.huseli.thoucylinder.pluralStringResource
import us.huseli.thoucylinder.umlautify
import java.util.UUID

@Composable
fun ArtistGrid(
    artistCombos: List<ArtistCombo>,
    progressIndicatorText: String? = null,
    onArtistClick: (UUID) -> Unit,
    onEmpty: @Composable (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(vertical = 10.dp),
    getImage: suspend (ArtistCombo) -> ImageBitmap?,
) {
    ItemGrid(
        things = artistCombos,
        onClick = { _, combo -> onArtistClick(combo.artist.id) },
        contentPadding = contentPadding,
        progressIndicatorText = progressIndicatorText,
        onEmpty = onEmpty,
    ) { _, combo ->
        var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

        LaunchedEffect(combo.artist.image) {
            imageBitmap = getImage(combo)
        }

        Thumbnail(
            image = imageBitmap,
            modifier = Modifier.fillMaxWidth(),
            borderWidth = null,
            placeholderIcon = Icons.Sharp.InterpreterMode,
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.padding(horizontal = 5.dp, vertical = 10.dp).weight(1f)) {
                Text(
                    text = combo.artist.name.umlautify(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = ThouCylinderTheme.typographyExtended.listSmallHeader,
                )
                Text(
                    style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                    text = pluralStringResource(R.plurals.x_tracks, combo.trackCount, combo.trackCount),
                )
            }
        }
    }
}
