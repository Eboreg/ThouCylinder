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
import us.huseli.retaintheme.sensibleFormat
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.dataclasses.AlbumPojo
import us.huseli.thoucylinder.viewmodels.BaseViewModel

@Composable
fun AlbumList(
    albums: List<AlbumPojo>,
    viewModel: BaseViewModel,
    onAlbumClick: (AlbumPojo) -> Unit,
    showArtist: Boolean = true,
) {
    ItemList(
        things = albums,
        onCardClick = onAlbumClick,
        selector = { pojo -> pojo.album.title },
    ) { pojo ->
        val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }
        val firstRow =
            if (showArtist && pojo.album.artist != null) "${pojo.album.artist} - ${pojo.album.title}"
            else pojo.album.title
        val secondRow = listOfNotNull(
            pojo.trackCount?.let { pluralStringResource(R.plurals.x_tracks, it, it) },
            pojo.yearString,
            pojo.duration?.sensibleFormat(),
        ).joinToString(" • ").takeIf { it.isNotBlank() }

        LaunchedEffect(Unit) {
            pojo.album.albumArt?.let { imageBitmap.value = viewModel.getImageBitmap(it) }
        }

        Row {
            AlbumArt(image = imageBitmap.value, modifier = Modifier.fillMaxHeight())
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                Text(text = firstRow, maxLines = 2, overflow = TextOverflow.Ellipsis)
                secondRow?.also {
                    Text(text = it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
