package us.huseli.thoucylinder.compose.screens.imports

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import us.huseli.retaintheme.extensions.sensibleFormat
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.compose.utils.ObnoxiousProgressIndicator
import us.huseli.thoucylinder.interfaces.IExternalAlbum
import us.huseli.thoucylinder.pluralStringResource
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify

@Composable
fun ImportItemList(
    albums: ImmutableList<IExternalAlbum>,
    importedAlbumIds: ImmutableMap<String, String>,
    isSearching: Boolean,
    onGotoAlbumClick: (String) -> Unit,
    toggleSelected: (String) -> Unit,
    onLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    isSelected: (String) -> Boolean = { false },
    isImported: (String) -> Boolean = { false },
    isNotFound: (String) -> Boolean = { false },
) {
    val context = LocalContext.current

    LaunchedEffect(albums.firstOrNull()) {
        if (albums.isNotEmpty()) listState.scrollToItem(0)
    }
    ItemList(
        modifier = modifier,
        things = albums,
        cardHeight = 60.dp,
        key = { _, state -> state.id },
        contentPadding = { PaddingValues(vertical = 5.dp) },
        listState = listState,
        gap = 5.dp,
        isSelected = { isSelected(it.id) },
        onClick = remember {
            { _, state ->
                val importedAlbumId = importedAlbumIds[state.id]

                if (importedAlbumId != null) onGotoAlbumClick(importedAlbumId)
                else toggleSelected(state.id)
            }
        },
        onLongClick = remember { { _, state -> onLongClick(state.id) } },
        trailingItem = { if (isSearching) ObnoxiousProgressIndicator() },
    ) { _, album ->
        var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

        LaunchedEffect(album) {
            imageBitmap =
                if (!isImported(album.id) && !isNotFound(album.id)) album.getThumbnailImageBitmap(context)
                else null
        }

        ImportableAlbumRow(
            imageBitmap = { imageBitmap },
            isImported = isImported(album.id),
            isNotFound = isNotFound(album.id),
            albumTitle = album.title,
            artist = album.artistName,
            thirdRow = {
                val strings = mutableListOf<String>()

                album.trackCount?.also { strings.add(pluralStringResource(R.plurals.x_tracks, it, it)) }
                album.year?.also { strings.add(it.toString()) }
                album.duration?.also { strings.add(it.sensibleFormat()) }
                album.playCount?.also { strings.add(stringResource(R.string.play_count, it)) }

                if (strings.isNotEmpty()) Text(
                    text = strings.joinToString(" • ").umlautify(),
                    style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                    maxLines = 1,
                )
            },
        )
    }
}
