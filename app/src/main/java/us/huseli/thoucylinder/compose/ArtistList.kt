package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.dataclasses.Album
import us.huseli.thoucylinder.dataclasses.Image
import us.huseli.thoucylinder.dataclasses.Track
import us.huseli.thoucylinder.viewmodels.LibraryViewModel

@Composable
fun ArtistList(
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
    artistTrackMap: Map<String, List<Track>>,
    images: Map<String, Image?>,
    albums: List<Album>,
    onArtistClick: ((String) -> Unit)? = null,
) {
    ItemList(
        things = artistTrackMap.toList(),
        modifier = modifier,
        onCardClick = onArtistClick?.let { { onArtistClick(it.first) } },
        selector = { (artist, _) -> artist },
    ) { (artist, tracks) ->
        val trackCount = tracks.size
        val albumCount = albums.filter { it.artist == artist }.size
        val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }

        LaunchedEffect(Unit) {
            images[artist]?.let { imageBitmap.value = viewModel.getImageBitmap(it) }
            if (imageBitmap.value == null) imageBitmap.value = albums
                .filter { it.artist == artist }
                .firstNotNullOfOrNull { it.albumArt?.getImageBitmap() }
        }

        Row {
            AlbumArt(image = imageBitmap.value, modifier = Modifier.fillMaxHeight())
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                Text(text = artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    style = MaterialTheme.typography.bodySmall,
                    text = pluralStringResource(R.plurals.x_albums, albumCount, albumCount) + " • " +
                        pluralStringResource(R.plurals.x_tracks, trackCount, trackCount),
                )
            }
        }
    }
}
