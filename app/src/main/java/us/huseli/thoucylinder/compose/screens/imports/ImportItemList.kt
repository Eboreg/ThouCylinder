package us.huseli.thoucylinder.compose.screens.imports

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.compose.utils.ObnoxiousProgressIndicator
import us.huseli.thoucylinder.interfaces.IExternalAlbum
import us.huseli.thoucylinder.viewmodels.AbstractImportViewModel
import java.util.UUID

@Composable
fun <A : IExternalAlbum> ImportItemList(
    viewModel: AbstractImportViewModel<A>,
    externalAlbums: List<A>,
    selectedExternalAlbumIds: List<String>,
    onGotoAlbumClick: (UUID) -> Unit,
    albumThirdRow: @Composable (A) -> Unit,
    listState: LazyListState = rememberLazyListState(),
) {
    val importedAlbumIds by viewModel.importedAlbumIds.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val notFoundAlbumIds by viewModel.notFoundAlbumIds.collectAsStateWithLifecycle()

    LaunchedEffect(externalAlbums.firstOrNull()) {
        if (externalAlbums.isNotEmpty()) listState.scrollToItem(0)
    }

    ItemList(
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
            else viewModel.toggleSelected(album)
        },
        onLongClick = { _, album ->
            viewModel.selectFromLastSelected(album.id, externalAlbums.map { it.id })
        },
        trailingItem = { if (isSearching) ObnoxiousProgressIndicator() },
    ) { _, album ->
        val imageBitmap = remember(album) { mutableStateOf<ImageBitmap?>(null) }
        val isImported = importedAlbumIds.contains(album.id)
        val isNotFound = notFoundAlbumIds.contains(album.id)

        LaunchedEffect(album, isImported, isNotFound) {
            imageBitmap.value =
                if (!isImported && !isNotFound) viewModel.getThumbnail(album)
                else null
        }

        ImportableAlbumRow(
            imageBitmap = imageBitmap.value,
            isImported = isImported,
            isNotFound = isNotFound,
            albumTitle = album.title,
            artist = album.artistName,
            thirdRow = { albumThirdRow(album) },
        )
    }
}
