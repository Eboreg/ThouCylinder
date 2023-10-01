package us.huseli.thoucylinder.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.Album
import us.huseli.thoucylinder.dataclasses.Image
import us.huseli.thoucylinder.viewmodels.LibraryViewModel

@Composable
fun ArtistList(
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
    images: Map<String, Image?>,
    albums: List<Album>,
    onArtistClick: ((String) -> Unit)? = null,
) {
    val artistsWithTracks by viewModel.artistsWithTracks.collectAsStateWithLifecycle(emptyMap())

    LazyColumn(
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier,
    ) {
        items(artistsWithTracks.toList()) { (artist, tracks) ->
            val trackCount = tracks.size
            val albumCount = albums.filter { it.artist == artist }.size
            val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }
            var cardModifier = Modifier.fillMaxWidth().height(80.dp)
            if (onArtistClick != null)
                cardModifier = cardModifier.clickable { onArtistClick(artist) }

            LaunchedEffect(Unit) {
                images[artist]?.let { imageBitmap.value = viewModel.getImageBitmap(it) }
                if (imageBitmap.value == null) imageBitmap.value = albums
                    .filter { it.artist == artist }
                    .firstNotNullOfOrNull { it.albumArt?.getImageBitmap() }
            }

            OutlinedCard(shape = MaterialTheme.shapes.extraSmall, modifier = cardModifier) {
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
    }
}