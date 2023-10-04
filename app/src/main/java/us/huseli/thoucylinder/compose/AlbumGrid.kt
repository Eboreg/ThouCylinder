package us.huseli.thoucylinder.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import us.huseli.thoucylinder.compose.utils.ItemGrid
import us.huseli.thoucylinder.dataclasses.AlbumPojo
import us.huseli.thoucylinder.viewmodels.BaseViewModel

@Composable
fun AlbumGrid(
    albums: List<AlbumPojo>,
    viewModel: BaseViewModel,
    modifier: Modifier = Modifier,
    onAlbumClick: (AlbumPojo) -> Unit,
) {
    ItemGrid(things = albums, modifier = modifier, onCardClick = onAlbumClick) { pojo ->
        val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }

        LaunchedEffect(Unit) {
            pojo.album.albumArt?.let { imageBitmap.value = viewModel.getImageBitmap(it) }
        }

        Box(modifier = Modifier.aspectRatio(1f)) {
            imageBitmap.value?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.aspectRatio(1f),
                )
            } ?: kotlin.run {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    pojo.album.artist?.let {
                        Text(
                            text = it,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Text(text = pojo.album.title, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
