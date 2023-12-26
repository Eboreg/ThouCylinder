package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.getActivity
import us.huseli.retaintheme.sensibleFormat
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.retaintheme.ui.theme.LocalBasicColors
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.CompactSearchTextField
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.compose.utils.ObnoxiousProgressIndicator
import us.huseli.thoucylinder.compose.utils.SmallButton
import us.huseli.thoucylinder.compose.utils.SmallOutlinedButton
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.ImportProgressData
import us.huseli.thoucylinder.viewmodels.SpotifyImportViewModel
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSpotify(
    modifier: Modifier = Modifier,
    viewModel: SpotifyImportViewModel = hiltViewModel(),
    listState: LazyListState = rememberLazyListState(),
    onGotoLibraryClick: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context.getActivity()

    val filteredUserAlbumCount by viewModel.filteredUserAlbumCount.collectAsStateWithLifecycle(null)
    val hasNext by viewModel.hasNext.collectAsStateWithLifecycle(false)
    val importedAlbumIds by viewModel.importedAlbumIds.collectAsStateWithLifecycle()
    val isAllSelected by viewModel.isAllSelected.collectAsStateWithLifecycle(false)
    val isAuthorized by viewModel.isAuthorized.collectAsStateWithLifecycle(false)
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val isUserAlbumCountExact by viewModel.isUserAlbumCountExact.collectAsStateWithLifecycle(false)
    val nextUserAlbumIdx by viewModel.nextUserAlbumIdx.collectAsStateWithLifecycle()
    val notFoundAlbumIds by viewModel.notFoundAlbumIds.collectAsStateWithLifecycle()
    val offset by viewModel.localOffset.collectAsStateWithLifecycle(0)
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val searchTerm by viewModel.searchTerm.collectAsStateWithLifecycle()
    val selectedUserAlbums by viewModel.selectedUserAlbums.collectAsStateWithLifecycle()
    val totalUserAlbumCount by viewModel.totalUserAlbumCount.collectAsStateWithLifecycle()
    val userAlbums by viewModel.offsetUserAlbums.collectAsStateWithLifecycle(emptyList())

    LaunchedEffect(isAuthorized) {
        if (isAuthorized) viewModel.setOffset(0)
    }

    LaunchedEffect(userAlbums.firstOrNull()) {
        if (userAlbums.isNotEmpty()) listState.scrollToItem(0)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        ImportSpotifyHeader(
            isAuthorized = isAuthorized,
            hasPrevious = offset > 0,
            hasNext = hasNext,
            offset = offset,
            currentAlbumCount = userAlbums.size,
            userAlbumCount = filteredUserAlbumCount
                ?.let { if (isUserAlbumCountExact) it.toString() else "≥ $it" } ?: "?",
            isAllSelected = isAllSelected,
            progress = progress,
            importButtonEnabled = progress == null && selectedUserAlbums.isNotEmpty(),
            selectAllEnabled = userAlbums.isNotEmpty(),
            onRefreshClick = { viewModel.setOffset(0) },
            onImportClick = {
                viewModel.importSelectedAlbums { importCount, notFoundCount ->
                    val strings = mutableListOf<String>()
                    if (importCount > 0) {
                        strings.add(
                            context.resources.getQuantityString(R.plurals.x_albums_imported, importCount, importCount)
                        )
                    }
                    if (notFoundCount > 0) {
                        strings.add(
                            context.resources.getQuantityString(
                                R.plurals.x_albums_not_found,
                                notFoundCount,
                                notFoundCount,
                            )
                        )
                    }
                    if (strings.isNotEmpty()) SnackbarEngine.addInfo(
                        message = strings.joinToString(" "),
                        actionLabel = context.getString(R.string.go_to_library),
                        onActionPerformed = onGotoLibraryClick,
                    )
                }
            },
            onPreviousClick = { viewModel.setOffset(max(offset - 50, 0)) },
            onNextClick = { viewModel.setOffset(offset + 50) },
            onSelectAllClick = { viewModel.setSelectAll(it) },
            onAuthorizeClick = { if (activity != null) viewModel.authorize(activity) },
            searchTerm = searchTerm,
            onSearch = { viewModel.setSearchTerm(it) },
        )

        ItemList(
            things = userAlbums,
            cardHeight = 50.dp,
            key = { _, pojo -> pojo.spotifyAlbum.id },
            gap = 5.dp,
            isSelected = { selectedUserAlbums.contains(it) },
            onClick = { _, pojo -> viewModel.toggleSelected(pojo) },
            listState = listState,
            contentPadding = PaddingValues(vertical = 5.dp),
            trailingItem = {
                if (isSearching) {
                    ObnoxiousProgressIndicator(
                        modifier = Modifier.padding(10.dp),
                        tonalElevation = 5.dp,
                        text = totalUserAlbumCount
                            ?.let { stringResource(R.string.loading_fraction_scream, nextUserAlbumIdx, it) }
                            ?: stringResource(R.string.loading_scream),
                    )
                }
            },
        ) { _, pojo ->
            val thumbnail = remember { mutableStateOf<ImageBitmap?>(null) }
            val isImported = importedAlbumIds.contains(pojo.spotifyAlbum.id)
            val isNotFound = notFoundAlbumIds.contains(pojo.spotifyAlbum.id)

            LaunchedEffect(pojo.spotifyAlbum.id, isImported, isNotFound) {
                if (!isImported && !isNotFound) thumbnail.value = viewModel.getThumbnail(pojo.spotifyAlbum, context)
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
                        text = "${pojo.artist} - ${pojo.spotifyAlbum.name}",
                        style = ThouCylinderTheme.typographyExtended.listSmallTitle,
                        maxLines = if (isImported || isNotFound) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isImported || isNotFound) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
                    )
                    if (isImported) {
                            Badge(
                                containerColor = LocalBasicColors.current.Green,
                                content = { Text(text = stringResource(R.string.imported)) },
                            )
                        } else if (isNotFound) {
                            Badge(
                                containerColor = LocalBasicColors.current.Red,
                                content = { Text(text = stringResource(R.string.no_match_found)) },
                            )
                    } else {
                        val count = pojo.spotifyTrackPojos.size

                        Text(
                            text = pluralStringResource(R.plurals.x_tracks, count, count) +
                                " • ${pojo.spotifyAlbum.year} • ${pojo.duration.sensibleFormat()}",
                            style = ThouCylinderTheme.typographyExtended.listNormalSubtitleSecondary,
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun ImportSpotifyHeader(
    modifier: Modifier = Modifier,
    isAuthorized: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    importButtonEnabled: Boolean,
    selectAllEnabled: Boolean,
    offset: Int,
    currentAlbumCount: Int,
    userAlbumCount: String,
    isAllSelected: Boolean,
    progress: ImportProgressData?,
    searchTerm: String,
    onRefreshClick: () -> Unit,
    onImportClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSelectAllClick: (Boolean) -> Unit,
    onAuthorizeClick: () -> Unit,
    onSearch: (String) -> Unit,
) {
    var isSearchFocused by rememberSaveable { mutableStateOf(false) }

    if (isAuthorized) {
        Surface(tonalElevation = 2.dp, color = BottomAppBarDefaults.containerColor) {
            ProvideTextStyle(value = ThouCylinderTheme.typographyExtended.listSmallTitle) {
                Column(
                    modifier = modifier.padding(horizontal = 10.dp).padding(bottom = 10.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        SmallButton(
                            onClick = onRefreshClick,
                            text = stringResource(R.string.refresh_spotify_albums)
                        )
                        SmallButton(
                            onClick = onImportClick,
                            text = stringResource(R.string.import_selected),
                            enabled = importButtonEnabled,
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (hasPrevious) {
                                SmallOutlinedButton(
                                    onClick = onPreviousClick,
                                    text = stringResource(R.string.previous)
                                )
                            }
                            if (currentAlbumCount > 0) {
                                Text(
                                    text = "${offset + 1} - ${currentAlbumCount + offset} ($userAlbumCount)",
                                    style = ThouCylinderTheme.typographyExtended.listSmallTitle,
                                )
                            }
                            if (hasNext) {
                                SmallOutlinedButton(onClick = onNextClick, text = stringResource(R.string.next))
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.select_all_visible),
                                style = ThouCylinderTheme.typographyExtended.listSmallTitle,
                            )
                            Checkbox(
                                checked = isAllSelected,
                                onCheckedChange = onSelectAllClick,
                                enabled = selectAllEnabled,
                            )
                        }
                    }

                    CompactSearchTextField(
                        value = searchTerm,
                        onSearch = onSearch,
                        onFocusChanged = { isSearchFocused = it.isFocused },
                        placeholderText = stringResource(R.string.search),
                    )

                    if (progress != null) {
                        val statusText = stringResource(progress.status.stringId)

                        Column(modifier = Modifier.padding(top = 10.dp, bottom = 5.dp)) {
                            Text(text = "$statusText ${progress.item} …")
                            LinearProgressIndicator(
                                progress = progress.progress.toFloat(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(horizontal = 30.dp, vertical = 10.dp),
        ) {
            FilledTonalButton(
                onClick = onAuthorizeClick,
                content = { Text(text = stringResource(R.string.get_spotify_albums)) },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(text = stringResource(R.string.spotify_import_help_1))
            Text(text = stringResource(R.string.spotify_import_help_2))
        }
    }
}
