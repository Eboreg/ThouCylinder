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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.ItemGrid
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.combos.ArtistPojo
import us.huseli.thoucylinder.pluralStringResource
import us.huseli.thoucylinder.umlautify

@Composable
fun ArtistGrid(
    artistPojos: List<ArtistPojo>,
    progressIndicatorText: String? = null,
    onArtistClick: (String) -> Unit,
    onEmpty: @Composable (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(vertical = 10.dp),
    imageFlow: (ArtistPojo) -> Flow<ImageBitmap?>,
) {
    ItemGrid(
        things = artistPojos,
        onClick = { _, artistPojo -> onArtistClick(artistPojo.name) },
        contentPadding = contentPadding,
        progressIndicatorText = progressIndicatorText,
        onEmpty = onEmpty,
    ) { _, artist ->
        val imageBitmap by imageFlow(artist).collectAsStateWithLifecycle(null)

        Thumbnail(
            image = imageBitmap,
            modifier = Modifier.fillMaxWidth(),
            borderWidth = null,
            placeholderIcon = Icons.Sharp.InterpreterMode,
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.padding(horizontal = 5.dp, vertical = 10.dp).weight(1f)) {
                Text(
                    text = artist.name.umlautify(),
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
