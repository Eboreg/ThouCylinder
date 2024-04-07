package us.huseli.thoucylinder.compose.screens.imports

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.compose.utils.ObnoxiousProgressIndicator
import us.huseli.thoucylinder.interfaces.IExternalAlbum

@Composable
fun <A : IExternalAlbum> ImportItemList(
    externalAlbums: ImmutableList<A>,
    importedAlbumIds: ImmutableMap<String, String>,
    isSearching: Boolean,
    notFoundAlbumIds: ImmutableList<String>,
    selectedExternalAlbumIds: ImmutableList<String>,
    onGotoAlbumClick: (String) -> Unit,
    albumThirdRow: @Composable (A) -> Unit,
    toggleSelected: (String) -> Unit,
    onLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val context = LocalContext.current

    LaunchedEffect(externalAlbums.firstOrNull()) {
        if (externalAlbums.isNotEmpty()) listState.scrollToItem(0)
    }

    ItemList(
        modifier = modifier,
        things = externalAlbums,
        cardHeight = 60.dp,
        key = { _, album -> album.id },
        contentPadding = PaddingValues(vertical = 5.dp),
        listState = listState,
        gap = 5.dp,
        isSelected = { selectedExternalAlbumIds.contains(it.id) },
        onClick = { _, album ->
            val importedAlbumId = importedAlbumIds[album.id]

            if (importedAlbumId != null) onGotoAlbumClick(importedAlbumId)
            else toggleSelected(album.id)
        },
        onLongClick = { _, album -> onLongClick(album.id) },
        trailingItem = { if (isSearching) ObnoxiousProgressIndicator() },
    ) { _, album ->
        var imageBitmap by remember(album) { mutableStateOf<ImageBitmap?>(null) }
        val isImported = importedAlbumIds.contains(album.id)
        val isNotFound = notFoundAlbumIds.contains(album.id)

        LaunchedEffect(album, isImported, isNotFound) {
            imageBitmap =
                if (!isImported && !isNotFound) album.getThumbnailImageBitmap(context)
                else null
        }

        ImportableAlbumRow(
            imageBitmap = { imageBitmap },
            isImported = isImported,
            isNotFound = isNotFound,
            albumTitle = album.title,
            artist = album.artistName,
            thirdRow = { albumThirdRow(album) },
        )
    }
}
