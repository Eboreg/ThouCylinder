package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.extensions.getActivity
import us.huseli.retaintheme.extensions.sensibleFormat
import us.huseli.retaintheme.isInLandscapeMode
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.screens.PaginationSection
import us.huseli.thoucylinder.compose.screens.ProgressSection
import us.huseli.thoucylinder.compose.screens.SelectAllCheckbox
import us.huseli.thoucylinder.compose.utils.CompactSearchTextField
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.compose.utils.ObnoxiousProgressIndicator
import us.huseli.thoucylinder.compose.utils.SmallButton
import us.huseli.thoucylinder.dataclasses.ImportProgressData
import us.huseli.thoucylinder.viewmodels.SpotifyImportViewModel
import kotlin.math.max

@Composable
fun ImportSpotify(
    modifier: Modifier = Modifier,
    viewModel: SpotifyImportViewModel = hiltViewModel(),
    listState: LazyListState = rememberLazyListState(),
    onGotoLibraryClick: () -> Unit,
    backendSelection: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val activity = context.getActivity()

    val filteredAlbumCount by viewModel.filteredAlbumCount.collectAsStateWithLifecycle(null)
    val hasNext by viewModel.hasNext.collectAsStateWithLifecycle(false)
    val importedAlbumIds by viewModel.importedAlbumIds.collectAsStateWithLifecycle()
    val isAllSelected by viewModel.isAllSelected.collectAsStateWithLifecycle(false)
    val isAuthorized by viewModel.isAuthorized.collectAsStateWithLifecycle(null)
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val isAlbumCountExact by viewModel.isAlbumCountExact.collectAsStateWithLifecycle(false)
    val nextAlbumIdx by viewModel.nextAlbumIdx.collectAsStateWithLifecycle()
    val notFoundAlbumIds by viewModel.notFoundAlbumIds.collectAsStateWithLifecycle()
    val offset by viewModel.localOffset.collectAsStateWithLifecycle(0)
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val searchTerm by viewModel.searchTerm.collectAsStateWithLifecycle()
    val selectedUserAlbums by viewModel.selectedAlbumPojos.collectAsStateWithLifecycle()
    val totalAlbumCount by viewModel.totalAlbumCount.collectAsStateWithLifecycle()
    val albumPojos by viewModel.offsetAlbumPojos.collectAsStateWithLifecycle(emptyList())

    LaunchedEffect(isAuthorized) {
        if (isAuthorized == true) viewModel.setOffset(0)
    }

    LaunchedEffect(albumPojos.firstOrNull()) {
        if (albumPojos.isNotEmpty()) listState.scrollToItem(0)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        ImportSpotifyHeader(
            isAuthorized = if (albumPojos.isEmpty()) isAuthorized else true,
            hasPrevious = offset > 0,
            hasNext = hasNext,
            importButtonEnabled = progress == null && selectedUserAlbums.isNotEmpty(),
            selectAllEnabled = albumPojos.isNotEmpty(),
            offset = offset,
            currentAlbumCount = albumPojos.size,
            totalAlbumCount = filteredAlbumCount,
            isTotalAlbumCountExact = isAlbumCountExact,
            isAllSelected = isAllSelected,
            progress = progress,
            searchTerm = searchTerm,
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
            onSearch = { viewModel.setSearchTerm(it) },
            onAuthorizeClick = { activity?.also { viewModel.authorize(it) } },
            backendSelection = backendSelection,
        )

        if (albumPojos.isEmpty()) {
            if (isAuthorized != true) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(horizontal = 30.dp, vertical = 10.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(text = stringResource(R.string.spotify_import_help_1))
                    Text(text = stringResource(R.string.spotify_import_help_2))
                }
            }
        }

        ItemList(
            things = albumPojos,
            cardHeight = 60.dp,
            key = { _, pojo -> pojo.spotifyAlbum.id },
            gap = 5.dp,
            isSelected = { selectedUserAlbums.contains(it) },
            onClick = { _, pojo -> viewModel.toggleSelected(pojo) },
            listState = listState,
            contentPadding = PaddingValues(vertical = 5.dp),
            trailingItem = {
                if (isSearching) {
                    ObnoxiousProgressIndicator(
                        text = totalAlbumCount
                            ?.let { stringResource(R.string.loading_fraction_scream, nextAlbumIdx, it) }
                            ?: stringResource(R.string.loading_scream),
                    )
                }
            },
        ) { _, pojo ->
            val imageBitmap = remember(pojo.spotifyAlbum) { mutableStateOf<ImageBitmap?>(null) }
            val isImported = importedAlbumIds.contains(pojo.spotifyAlbum.id)
            val isNotFound = notFoundAlbumIds.contains(pojo.spotifyAlbum.id)

            LaunchedEffect(pojo.spotifyAlbum, isImported, isNotFound) {
                if (!isImported && !isNotFound) imageBitmap.value = viewModel.getThumbnail(pojo.spotifyAlbum, context)
                else imageBitmap.value = null
            }

            ImportableAlbumRow(
                imageBitmap = imageBitmap.value,
                isImported = isImported,
                isNotFound = isNotFound,
                albumTitle = pojo.spotifyAlbum.name,
                artist = pojo.artist,
                thirdRow = {
                    val count = pojo.spotifyTrackPojos.size

                    Text(
                        text = pluralStringResource(R.plurals.x_tracks, count, count) +
                            " • ${pojo.spotifyAlbum.year} • ${pojo.duration.sensibleFormat()}",
                        style = ThouCylinderTheme.typographyExtended.listNormalSubtitleSecondary,
                    )
                }
            )
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImportSpotifyHeader(
    modifier: Modifier = Modifier,
    isAuthorized: Boolean?,
    hasPrevious: Boolean,
    hasNext: Boolean,
    importButtonEnabled: Boolean,
    selectAllEnabled: Boolean,
    offset: Int,
    currentAlbumCount: Int,
    totalAlbumCount: Int?,
    isTotalAlbumCountExact: Boolean,
    isAllSelected: Boolean,
    progress: ImportProgressData?,
    searchTerm: String,
    onImportClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSelectAllClick: (Boolean) -> Unit,
    onSearch: (String) -> Unit,
    onAuthorizeClick: () -> Unit,
    backendSelection: @Composable () -> Unit,
) {
    val isLandscape = isInLandscapeMode()

    Surface(tonalElevation = 2.dp, color = BottomAppBarDefaults.containerColor) {
        ProvideTextStyle(value = ThouCylinderTheme.typographyExtended.listNormalTitle) {
            Column(
                modifier = modifier
                    .padding(horizontal = 10.dp)
                    .padding(bottom = 10.dp, top = if (isLandscape) 10.dp else 0.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = if (isLandscape) 3 else 2,
                ) {
                    backendSelection()

                    if (isAuthorized == true) {
                        if (!isLandscape) {
                            SmallButton(
                                onClick = onImportClick,
                                text = stringResource(R.string.import_str),
                                enabled = importButtonEnabled,
                            )
                        }

                        PaginationSection(
                            currentAlbumCount = currentAlbumCount,
                            offset = offset,
                            totalAlbumCount = totalAlbumCount,
                            isTotalAlbumCountExact = isTotalAlbumCountExact,
                            hasPrevious = hasPrevious,
                            hasNext = hasNext,
                            onPreviousClick = onPreviousClick,
                            onNextClick = onNextClick,
                        )

                        if (!isLandscape) {
                            SelectAllCheckbox(
                                checked = isAllSelected,
                                enabled = selectAllEnabled,
                                onCheckedChange = onSelectAllClick,
                                modifier = Modifier.align(Alignment.CenterVertically),
                            )
                        }

                        if (isLandscape) {
                            SmallButton(
                                onClick = onImportClick,
                                text = stringResource(R.string.import_str),
                                enabled = importButtonEnabled,
                            )
                        }

                        CompactSearchTextField(
                            value = searchTerm,
                            onSearch = onSearch,
                            placeholderText = stringResource(R.string.search),
                            modifier = Modifier.weight(1f),
                        )

                        if (isLandscape) {
                            SelectAllCheckbox(
                                checked = isAllSelected,
                                enabled = selectAllEnabled,
                                onCheckedChange = onSelectAllClick,
                                modifier = Modifier.align(Alignment.CenterVertically).padding(start = 10.dp),
                            )
                        }
                    } else {
                        SmallButton(onClick = onAuthorizeClick, text = stringResource(R.string.authorize))
                    }
                }

                ProgressSection(progress = progress)
            }
        }
    }
}
