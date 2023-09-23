package us.huseli.thoucylinder.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import us.huseli.thoucylinder.LoadStatus
import us.huseli.thoucylinder.R

@Composable
fun AlbumArt(
    image: ImageBitmap?,
    modifier: Modifier = Modifier,
    loadStatus: LoadStatus? = null,
    topContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Box(modifier = modifier.aspectRatio(1f)) {
        image?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier.aspectRatio(1f),
            )
        } ?: kotlin.run {
            Image(
                imageVector = Icons.Sharp.Album,
                contentDescription = null,
                modifier = modifier.aspectRatio(1f),
            )
        }
        if (loadStatus == LoadStatus.LOADING) {
            ObnoxiousProgressIndicator(
                text = stringResource(R.string.loading_album_art_scream),
                modifier = Modifier.align(Alignment.Center),
            )
        }
        topContent?.let { content ->
            Column(modifier = Modifier.fillMaxWidth().align(Alignment.TopStart), content = content)
        }
    }
}
