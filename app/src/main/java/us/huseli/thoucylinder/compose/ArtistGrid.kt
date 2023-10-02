package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.ItemGrid
import us.huseli.thoucylinder.dataclasses.Album
import us.huseli.thoucylinder.dataclasses.Image
import us.huseli.thoucylinder.dataclasses.Track
import us.huseli.thoucylinder.viewmodels.LibraryViewModel

@Composable
fun ArtistGrid(
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
    artistTrackMap: Map<String, List<Track>>,
    images: Map<String, Image?>,
    albums: List<Album>,
    onArtistClick: (String) -> Unit,
) {
    ItemGrid(
        things = artistTrackMap.toList(),
        modifier = modifier,
        onCardClick = { onArtistClick(it.first) },
    ) { (artist, tracks) ->
        val trackCount = tracks.size
        val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }

        LaunchedEffect(Unit) {
            images[artist]?.let { imageBitmap.value = viewModel.getImageBitmap(it) }
            if (imageBitmap.value == null) imageBitmap.value = albums
                .filter { it.artist == artist }
                .firstNotNullOfOrNull { it.albumArt?.getImageBitmap() }
        }

        Box(modifier = Modifier.aspectRatio(1f)) {
            AlbumArt(image = imageBitmap.value, modifier = Modifier.fillMaxWidth())
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.padding(5.dp).weight(1f)) {
                Text(text = artist, maxLines = 2)
                Text(
                    style = MaterialTheme.typography.bodySmall,
                    text = pluralStringResource(R.plurals.x_tracks, trackCount, trackCount),
                )
            }
        }
    }
}
