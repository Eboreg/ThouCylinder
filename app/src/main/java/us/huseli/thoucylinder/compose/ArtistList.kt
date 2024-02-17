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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import us.huseli.retaintheme.extensions.sensibleFormat
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.combos.ArtistPojo
import us.huseli.thoucylinder.pluralStringResource
import us.huseli.thoucylinder.umlautify

@Composable
fun ArtistList(
    artistPojos: List<ArtistPojo>,
    progressIndicatorText: String? = null,
    onArtistClick: (String) -> Unit,
    onEmpty: @Composable (() -> Unit)? = null,
    imageFlow: (ArtistPojo) -> Flow<ImageBitmap?>,
) {
    ItemList(
        things = artistPojos,
        progressIndicatorText = progressIndicatorText,
        onClick = { _, artistPojo -> onArtistClick(artistPojo.name) },
        onEmpty = onEmpty,
    ) { _, artist ->
        val imageBitmap by imageFlow(artist).collectAsStateWithLifecycle(null)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Thumbnail(
                image = imageBitmap,
                shape = MaterialTheme.shapes.extraSmall,
                placeholderIcon = Icons.Sharp.InterpreterMode,
            )
            Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = artist.name.umlautify(),
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
