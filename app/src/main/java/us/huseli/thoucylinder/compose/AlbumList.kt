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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.sensibleFormat
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.Album
import us.huseli.thoucylinder.viewmodels.BaseViewModel

@Composable
fun AlbumList(albums: List<Album>, viewModel: BaseViewModel, onAlbumClick: (Album) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        items(albums) { album ->
            val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }

            LaunchedEffect(Unit) {
                album.albumArt?.let { imageBitmap.value = viewModel.getImageBitmap(it) }
            }

            OutlinedCard(
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.fillMaxWidth().height(80.dp).clickable { onAlbumClick(album) },
            ) {
                Row {
                    AlbumArt(image = imageBitmap.value, modifier = Modifier.fillMaxHeight())
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp).fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Text(text = album.toString(), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            style = MaterialTheme.typography.bodySmall,
                            text = listOfNotNull(
                                pluralStringResource(R.plurals.x_tracks, album.trackCount, album.trackCount),
                                album.yearString,
                                album.duration?.sensibleFormat(),
                            ).joinToString(" • ")
                        )
                    }
                }
            }
        }
    }
}
