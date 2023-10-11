package us.huseli.thoucylinder.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.sensibleFormat
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.dataclasses.pojos.AlbumPojo
import us.huseli.thoucylinder.viewmodels.BaseViewModel

@Composable
fun AlbumList(
    pojos: List<AlbumPojo>,
    viewModel: BaseViewModel,
    onAlbumClick: (AlbumPojo) -> Unit,
    onAddToPlaylistClick: (Selection) -> Unit,
    modifier: Modifier = Modifier,
    listModifier: Modifier = Modifier,
    showArtist: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(vertical = 10.dp),
) {
    val selectedAlbums by viewModel.selectedAlbums.collectAsStateWithLifecycle()
    val selectOnShortClick = selectedAlbums.isNotEmpty()

    Column(modifier = modifier.fillMaxWidth()) {
        SelectedAlbumsButtons(
            albumCount = selectedAlbums.size,
            onPlayClick = { viewModel.playAlbums(selectedAlbums.map { it.albumId }) },
            onAddToPlaylistClick = { onAddToPlaylistClick(Selection(albums = selectedAlbums)) },
            onUnselectAllClick = { viewModel.unselectAllAlbums() }
        )

        ItemList(
            things = pojos,
            isSelected = { pojo -> selectedAlbums.contains(pojo.album) },
            onClick = { pojo ->
                if (selectOnShortClick) viewModel.toggleSelected(pojo.album)
                else onAlbumClick(pojo)
            },
            onLongClick = { pojo -> viewModel.toggleSelected(pojo.album) },
            modifier = listModifier,
            contentPadding = contentPadding,
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

            LaunchedEffect(pojo.album.albumId) {
                pojo.album.albumArt?.let {
                    imageBitmap.value = viewModel.getImageBitmap(it)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Thumbnail(
                    image = imageBitmap.value,
                    modifier = Modifier.fillMaxHeight(),
                    shape = MaterialTheme.shapes.extraSmall,
                    placeholder = {
                        Image(
                            imageVector = Icons.Sharp.Album,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().aspectRatio(1f),
                        )
                    }
                )
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp).fillMaxHeight().weight(1f),
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Text(text = firstRow, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    secondRow?.also {
                        Text(text = it, style = MaterialTheme.typography.bodySmall)
                    }
                }
                IconButton(
                    onClick = { viewModel.playAlbum(pojo.album.albumId) },
                    content = { Icon(Icons.Sharp.PlayArrow, stringResource(R.string.play)) },
                )
            }
        }
    }
}
