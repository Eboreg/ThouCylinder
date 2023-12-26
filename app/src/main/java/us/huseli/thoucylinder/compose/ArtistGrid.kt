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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.ItemGrid
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.viewmodels.ArtistListViewModel

@Composable
fun ArtistGrid(
    isImporting: Boolean,
    viewModel: ArtistListViewModel = hiltViewModel(),
    onArtistClick: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(vertical = 10.dp),
) {
    val context = LocalContext.current
    val artistPojos by viewModel.artistPojos.collectAsStateWithLifecycle(emptyList())
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    ItemGrid(
        things = artistPojos,
        onClick = { _, artist -> onArtistClick(artist.name) },
        contentPadding = contentPadding,
        key = { _, artist -> artist.name },
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

        Thumbnail(
            image = imageBitmap,
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
