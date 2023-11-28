package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material.icons.sharp.Cancel
import androidx.compose.material.icons.sharp.CheckCircle
import androidx.compose.material3.Badge
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.compose.SmallOutlinedButton
import us.huseli.retaintheme.getActivity
import us.huseli.retaintheme.sensibleFormat
import us.huseli.retaintheme.ui.theme.LocalBasicColors
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.compose.utils.SmallButton
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.ImportProgressData
import us.huseli.thoucylinder.viewmodels.SpotifyImportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    modifier: Modifier = Modifier,
    viewModel: SpotifyImportViewModel = hiltViewModel(),
    listState: LazyListState = rememberLazyListState(),
) {
    val pojos by viewModel.pojos.collectAsStateWithLifecycle(emptyList())
    val hasPrevious by viewModel.hasPrevious.collectAsStateWithLifecycle(false)
    val hasNext by viewModel.hasNext.collectAsStateWithLifecycle(false)
    val offset by viewModel.offset.collectAsStateWithLifecycle(0)
    val isAllSelected by viewModel.isAllSelected.collectAsStateWithLifecycle(false)
    val selectedPojos by viewModel.selectedPojos.collectAsStateWithLifecycle()
    val isAuthorized by viewModel.isAuthorized.collectAsStateWithLifecycle(false)
    val totalAlbumCount by viewModel.totalAlbumCount.collectAsStateWithLifecycle(initialValue = 0)
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val importedAlbumIds by viewModel.importedAlbumIds.collectAsStateWithLifecycle()
    val notFoundAlbumIds by viewModel.notFoundAlbumIds.collectAsStateWithLifecycle()
    val activity = LocalContext.current.getActivity()

    LaunchedEffect(isAuthorized) {
        if (isAuthorized) viewModel.fetchAlbums(offset)
    }

    LaunchedEffect(pojos.firstOrNull()) {
        if (pojos.isNotEmpty()) listState.scrollToItem(0)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        ItemList(
            things = pojos,
            cardHeight = 50.dp,
            key = { it.spotifyAlbum.id },
            gap = 5.dp,
            showNumericBarAtItemCount = 100,
            isSelected = { selectedPojos.contains(it) },
            onClick = { viewModel.toggleSelected(it) },
            listState = listState,
            stickyHeaderContent = {
                ImportScreenStickyHeader(
                    isAuthorized = isAuthorized,
                    hasPrevious = hasPrevious,
                    hasNext = hasNext,
                    offset = offset,
                    currentAlbumCount = pojos.size,
                    totalAlbumCount = totalAlbumCount,
                    isAllSelected = isAllSelected,
                    progress = progress,
                    importButtonEnabled = progress == null && selectedPojos.isNotEmpty(),
                    selectAllEnabled = pojos.isNotEmpty(),
                    onRefreshClick = { viewModel.fetchAlbums(0) },
                    onImportClick = { viewModel.importSelectedAlbums() },
                    onPreviousClick = { viewModel.fetchAlbums(offset - 50) },
                    onNextClick = { viewModel.fetchAlbums(offset + 50) },
                    onSelectAllClick = { viewModel.setSelectAll(it) },
                    onAuthorizeClick = { if (activity != null) viewModel.authorize(activity) },
                )
            }
        ) { pojo ->
            val thumbnail = remember { mutableStateOf<ImageBitmap?>(null) }
            val isImported = importedAlbumIds.contains(pojo.spotifyAlbum.id)
            val isNotFound = notFoundAlbumIds.contains(pojo.spotifyAlbum.id)

            LaunchedEffect(pojo.spotifyAlbum.id, isImported, isNotFound) {
                if (!isImported && !isNotFound) thumbnail.value = viewModel.getThumbnail(pojo.spotifyAlbum)
                else thumbnail.value = null
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Thumbnail(
                    image = thumbnail.value,
                    shape = MaterialTheme.shapes.extraSmall,
                    placeholderIcon = when {
                        isImported -> Icons.Sharp.CheckCircle
                        isNotFound -> Icons.Sharp.Cancel
                        else -> Icons.Sharp.Album
                    },
                    placeholderIconTint = when {
                        isImported -> LocalBasicColors.current.Green
                        isNotFound -> LocalBasicColors.current.Red
                        else -> null
                    },
                )
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Text(
                        text = "${pojo.artists.joinToString("/") { it.name }} - ${pojo.spotifyAlbum.name}",
                        style = ThouCylinderTheme.typographyExtended.listSmallTitle,
                        maxLines = if (isImported || isNotFound) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isImported || isNotFound) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
                    )
                    if (isImported) {
                        Badge(
                            containerColor = LocalBasicColors.current.Green,
                            content = { Text(text = "Imported") },
                        )
                    } else if (isNotFound) {
                        Badge(
                            containerColor = LocalBasicColors.current.Red,
                            content = { Text(text = "No match found") },
                        )
                    } else {
                        Text(
                            text = "${pojo.spotifyTrackPojos.size} tracks • ${pojo.spotifyAlbum.year} • ${pojo.duration.sensibleFormat()}",
                            style = ThouCylinderTheme.typographyExtended.listNormalSubtitleSecondary,
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun ImportScreenStickyHeader(
    modifier: Modifier = Modifier,
    isAuthorized: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    importButtonEnabled: Boolean,
    selectAllEnabled: Boolean,
    offset: Int,
    currentAlbumCount: Int,
    totalAlbumCount: Int,
    isAllSelected: Boolean,
    progress: ImportProgressData?,
    onRefreshClick: () -> Unit,
    onImportClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSelectAllClick: (Boolean) -> Unit,
    onAuthorizeClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(top = 5.dp)) {
        if (isAuthorized) {
            ProvideTextStyle(value = ThouCylinderTheme.typographyExtended.listSmallTitle) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background),
                ) {
                    SmallButton(onClick = onRefreshClick, text = "Refresh Spotify albums")
                    SmallButton(onClick = onImportClick, text = "Import selected", enabled = importButtonEnabled)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.background(MaterialTheme.colorScheme.background),
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (hasPrevious) {
                            SmallOutlinedButton(onClick = onPreviousClick, text = "Previous")
                        }
                        if (currentAlbumCount > 0) {
                            Text(
                                text = "${offset + 1} - ${currentAlbumCount + offset} ($totalAlbumCount)",
                                style = ThouCylinderTheme.typographyExtended.listSmallTitle,
                            )
                        }
                        if (hasNext) {
                            SmallOutlinedButton(onClick = onNextClick, text = "Next")
                        }
                    }
                    Text(text = "Select all", style = ThouCylinderTheme.typographyExtended.listSmallTitle)
                    Checkbox(checked = isAllSelected, onCheckedChange = onSelectAllClick, enabled = selectAllEnabled)
                }

                if (progress != null) {
                    val statusText = stringResource(progress.status.stringId)

                    Column(modifier = Modifier.padding(bottom = 5.dp)) {
                        Text(text = "$statusText ${progress.item} …")
                        LinearProgressIndicator(
                            progress = progress.progress.toFloat(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                OutlinedButton(
                    onClick = onAuthorizeClick,
                    content = { Text(text = "Get Spotify albums") },
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Text(text = stringResource(R.string.spotify_import_help_1))
                Text(text = stringResource(R.string.spotify_import_help_2))
            }
        }
    }
}
