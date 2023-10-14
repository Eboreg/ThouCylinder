package us.huseli.thoucylinder.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material.icons.sharp.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.sensibleFormat
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.pojos.AlbumPojo
import us.huseli.thoucylinder.viewmodels.BaseViewModel

@Composable
fun AlbumList(
    pojos: List<AlbumPojo>,
    viewModel: BaseViewModel,
    onAlbumClick: (AlbumPojo) -> Unit,
    onAddToPlaylistClick: (Selection) -> Unit,
    showArtist: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(vertical = 10.dp),
    onArtistClick: ((String) -> Unit)? = null,
) {
    val selectedAlbums by viewModel.selectedAlbums.collectAsStateWithLifecycle()
    val selectOnShortClick = selectedAlbums.isNotEmpty()
    val isSelected = { pojo: AlbumPojo -> selectedAlbums.contains(pojo.album) }
    val context = LocalContext.current

    Column {
        SelectedAlbumsButtons(
            albumCount = selectedAlbums.size,
            onPlayClick = { viewModel.playAlbums(selectedAlbums.map { it.albumId }) },
            onAddToPlaylistClick = { onAddToPlaylistClick(Selection(albums = selectedAlbums)) },
            onUnselectAllClick = { viewModel.unselectAllAlbums() }
        )

        ItemList(
            things = pojos,
            isSelected = isSelected,
            onClick = { pojo ->
                if (selectOnShortClick) viewModel.toggleSelected(pojo.album)
                else onAlbumClick(pojo)
            },
            onLongClick = { pojo -> viewModel.toggleSelected(pojo.album) },
            contentPadding = contentPadding,
        ) { pojo ->
            var isContextMenuOpen by rememberSaveable { mutableStateOf(false) }
            val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }
            val thirdRow = listOfNotNull(
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
                    shape = MaterialTheme.shapes.extraSmall,
                    placeholder = { Image(Icons.Sharp.Album, null) },
                    borderWidth = if (isSelected(pojo)) 0.dp else 1.dp,
                )
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp).fillMaxHeight().weight(1f),
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Text(
                        text = pojo.album.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = ThouCylinderTheme.typographyExtended.listNormalHeader,
                    )
                    if (pojo.album.artist != null && showArtist) {
                        Text(
                            text = pojo.album.artist,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = ThouCylinderTheme.typographyExtended.listNormalSubtitle,
                        )
                    }
                    if (thirdRow != null) {
                        Text(text = thirdRow, style = ThouCylinderTheme.typographyExtended.listNormalSubtitleSecondary)
                    }
                }

                Column {
                    IconButton(
                        onClick = { isContextMenuOpen = !isContextMenuOpen },
                        content = { Icon(Icons.Sharp.MoreVert, null) },
                    )
                    AlbumContextMenu(
                        album = pojo.album,
                        expanded = isContextMenuOpen,
                        onDismissRequest = { isContextMenuOpen = false },
                        onAddToPlaylistClick = { onAddToPlaylistClick(Selection(album = pojo.album)) },
                        onEnqueueNextClick = { viewModel.enqueueAlbumNext(album = pojo.album, context = context) },
                        onPlayClick = { viewModel.playAlbum(pojo.album.albumId) },
                        onArtistClick = onArtistClick,
                    )
                }
            }
        }
    }
}
