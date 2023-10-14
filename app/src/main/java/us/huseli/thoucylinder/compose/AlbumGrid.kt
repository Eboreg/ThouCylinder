package us.huseli.thoucylinder.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.ItemGrid
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.pojos.AlbumPojo
import us.huseli.thoucylinder.viewmodels.BaseViewModel

@Composable
fun AlbumGrid(
    albums: List<AlbumPojo>,
    viewModel: BaseViewModel,
    onAlbumClick: (AlbumPojo) -> Unit,
    contentPadding: PaddingValues = PaddingValues(vertical = 10.dp),
) {
    ItemGrid(things = albums, onClick = onAlbumClick, contentPadding = contentPadding) { pojo ->
        val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }

        LaunchedEffect(Unit) {
            pojo.album.albumArt?.let { imageBitmap.value = viewModel.getImageBitmap(it) }
        }

        Thumbnail(
            image = imageBitmap.value,
            placeholder = {
                Image(
                    imageVector = Icons.Sharp.Album,
                    contentDescription = null,
                    alpha = 0.5f,
                )
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = pojo.album.title,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        style = ThouCylinderTheme.typographyExtended.listNormalHeader,
                    )
                    pojo.album.artist?.also { artist ->
                        Text(
                            text = artist,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            style = ThouCylinderTheme.typographyExtended.listNormalTitle,
                        )
                    }
                }
            }
        )
    }
}
