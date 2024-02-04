package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.InterpreterMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.extensions.sensibleFormat
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.viewmodels.ArtistListViewModel

@Composable
fun ArtistList(
    isImporting: Boolean,
    viewModel: ArtistListViewModel = hiltViewModel(),
    onArtistClick: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val artistPojos by viewModel.artistPojos.collectAsStateWithLifecycle(emptyList())
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    ItemList(
        things = artistPojos,
        onClick = onArtistClick?.let { { _, artist -> onArtistClick(artist.name) } },
        onEmpty = {
            Text(
                stringResource(
                    if (isImporting) R.string.importing_local_artists
                    else if (isLoading) R.string.loading_artists
                    else R.string.no_artists_found
                ),
                modifier = Modifier.padding(10.dp),
            )
        },
    ) { _, artist ->
        val imageBitmap by viewModel.flowArtistImage(artist, context).collectAsStateWithLifecycle()

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Thumbnail(
                image = imageBitmap,
                shape = MaterialTheme.shapes.extraSmall,
                placeholderIcon = Icons.Sharp.InterpreterMode,
            )
            Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
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
