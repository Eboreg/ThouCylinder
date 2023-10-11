package us.huseli.thoucylinder.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.InterpreterMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.ItemGrid
import us.huseli.thoucylinder.dataclasses.pojos.ArtistPojo
import us.huseli.thoucylinder.dataclasses.Image
import us.huseli.thoucylinder.toBitmap
import us.huseli.thoucylinder.viewmodels.LibraryViewModel

@Composable
fun ArtistGrid(
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
    artists: List<ArtistPojo>,
    images: Map<String, Image?>,
    onArtistClick: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(vertical = 10.dp),
) {
    ItemGrid(
        things = artists,
        modifier = modifier,
        onClick = { onArtistClick(it.name) },
        contentPadding = contentPadding,
    ) { artist ->
        val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }

        LaunchedEffect(Unit) {
            images[artist.name.lowercase()]?.let { imageBitmap.value = viewModel.getImageBitmap(it) }
            if (imageBitmap.value == null) imageBitmap.value = artist.firstAlbumArt?.toBitmap()?.asImageBitmap()
        }

        Thumbnail(
            image = imageBitmap.value,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Image(
                    imageVector = Icons.Sharp.InterpreterMode,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().aspectRatio(1f),
                )
            },
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.padding(5.dp).weight(1f)) {
                Text(text = artist.name, maxLines = 2)
                Text(
                    style = MaterialTheme.typography.bodySmall,
                    text = pluralStringResource(R.plurals.x_tracks, artist.trackCount, artist.trackCount),
                )
            }
        }
    }
}
