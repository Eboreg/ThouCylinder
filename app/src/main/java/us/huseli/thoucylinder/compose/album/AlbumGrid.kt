package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material.icons.sharp.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import us.huseli.retaintheme.ui.theme.LocalBasicColors
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.ItemGrid
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.uistates.AlbumUiState
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.ImageViewModel

@Composable
fun AlbumGrid(
    states: () -> ImmutableList<AlbumUiState>,
    callbacks: AlbumCallbacks,
    selectionCallbacks: AlbumSelectionCallbacks,
    selectedAlbumIds: ImmutableList<String>,
    modifier: Modifier = Modifier,
    imageViewModel: ImageViewModel = hiltViewModel(),
    showArtist: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(vertical = 10.dp),
    progressIndicatorText: () -> String? = { null },
    onEmpty: @Composable () -> Unit,
) {
    val isSelected: (AlbumUiState) -> Boolean = { selectedAlbumIds.contains(it.albumId) }

    SelectedAlbumsButtons(albumCount = selectedAlbumIds.size, callbacks = selectionCallbacks)

    ItemGrid(
        modifier = modifier,
        things = states(),
        onClick = { _, state ->
            callbacks.onAlbumClick?.invoke(state.albumId)
        },
        onLongClick = { _, state ->
            callbacks.onAlbumLongClick?.invoke(state.albumId)
        },
        contentPadding = contentPadding,
        onEmpty = onEmpty,
        isSelected = isSelected,
        progressIndicatorText = progressIndicatorText,
    ) { _, state ->
        val artistString = state.artists.joined()
        val downloadState by state.downloadState.collectAsStateWithLifecycle()

        Box(modifier = Modifier.aspectRatio(1f)) {
            var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

            LaunchedEffect(state.fullImageUri) {
                imageBitmap = imageViewModel.getFullImageBitmap(state.fullImageUri)
            }

            Thumbnail(
                imageBitmap = { imageBitmap },
                borderWidth = null,
                placeholderIcon = Icons.Sharp.Album,
            )

            if (isSelected(state)) {
                Icon(
                    imageVector = Icons.Sharp.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    tint = LocalBasicColors.current.Green.copy(alpha = 0.7f),
                )
            }
        }

        downloadState?.also {
            if (it.isActive) LinearProgressIndicator(
                progress = { it.progress },
                modifier = Modifier.fillMaxWidth().height(2.dp),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = if (downloadState?.isActive == true) 3.dp else 5.dp,
                    bottom = 5.dp,
                    start = 5.dp,
                )
                .height(62.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = state.title.umlautify(),
                    maxLines = if (artistString != null && showArtist) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    style = ThouCylinderTheme.typographyExtended.listNormalHeader,
                )
                if (showArtist && artistString != null) Text(
                    text = artistString.umlautify(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = ThouCylinderTheme.typographyExtended.listSmallTitle,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AlbumSmallIcons(isLocal = state.isLocal, isOnYoutube = state.isOnYoutube)
                }
            }

            AlbumContextMenuWithButton(
                albumId = state.albumId,
                albumArtists = state.artists.toImmutableList(),
                isLocal = state.isLocal,
                isInLibrary = state.isInLibrary,
                isPartiallyDownloaded = state.isPartiallyDownloaded,
                callbacks = callbacks,
                spotifyWebUrl = state.spotifyWebUrl,
                youtubeWebUrl = state.youtubeWebUrl,
            )
        }
    }
}
