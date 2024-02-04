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
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import us.huseli.thoucylinder.viewmodels.LastFmViewModel
import kotlin.math.max

@Composable
fun ImportLastFm(
    modifier: Modifier = Modifier,
    viewModel: LastFmViewModel = hiltViewModel(),
    listState: LazyListState = rememberLazyListState(),
    onGotoSettingsClick: () -> Unit,
    onGotoLibraryClick: () -> Unit,
    backendSelection: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val hasNext by viewModel.hasNext.collectAsStateWithLifecycle(false)
    val importedAlbumIds by viewModel.importedAlbumIds.collectAsStateWithLifecycle()
    val isAllSelected by viewModel.isAllSelected.collectAsStateWithLifecycle(false)
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val notFoundAlbumIds by viewModel.notFoundAlbumIds.collectAsStateWithLifecycle()
    val offset by viewModel.offset.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val searchTerm by viewModel.searchTerm.collectAsStateWithLifecycle()
    val selectedTopAlbums by viewModel.selectedTopAlbums.collectAsStateWithLifecycle()
    val topAlbums by viewModel.offsetTopAlbums.collectAsStateWithLifecycle(emptyList())
    val totalAlbumCount by viewModel.totalFilteredAlbumCount.collectAsStateWithLifecycle(0)
    val username by viewModel.username.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.setOffset(0)
    }

    LaunchedEffect(topAlbums.firstOrNull()) {
        if (topAlbums.isNotEmpty()) listState.scrollToItem(0)
    }

    Column(modifier = modifier) {
        ImportLastFmHeader(
            hasPrevious = offset > 0,
            hasNext = hasNext,
            offset = offset,
            currentAlbumCount = topAlbums.size,
            searchTerm = searchTerm,
            onPreviousClick = { viewModel.setOffset(max(offset - 50, 0)) },
            onNextClick = { viewModel.setOffset(offset + 50) },
            onSearch = { viewModel.setSearchTerm(it) },
            totalAlbumCount = totalAlbumCount,
            isAllSelected = isAllSelected,
            selectAllEnabled = topAlbums.isNotEmpty(),
            onSelectAllClick = { viewModel.setSelectAll(it) },
            onImportClick = {
                viewModel.importSelectedTopAlbums { importCount, notFoundCount ->
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
            importButtonEnabled = progress == null && selectedTopAlbums.isNotEmpty(),
            progress = progress,
            backendSelection = backendSelection,
        )

        ItemList(
            key = { _, topAlbum -> topAlbum.mbid },
            cardHeight = 60.dp,
            things = topAlbums,
            contentPadding = PaddingValues(vertical = 5.dp),
            listState = listState,
            isSelected = { selectedTopAlbums.contains(it) },
            onClick = { _, topAlbum -> viewModel.toggleSelected(topAlbum) },
            trailingItem = {
                if (isSearching) {
                    ObnoxiousProgressIndicator()
                }
                if (username == null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.you_need_to_configure_your_last_fm_username_in_the_settings),
                            textAlign = TextAlign.Center,
                        )
                        OutlinedButton(
                            onClick = onGotoSettingsClick,
                            content = { Text(text = stringResource(R.string.go_to_settings)) },
                            shape = MaterialTheme.shapes.extraSmall,
                        )
                    }
                }
            },
        ) { _, album ->
            val imageBitmap = remember(album) { mutableStateOf<ImageBitmap?>(null) }
            val isImported = importedAlbumIds.contains(album.mbid)
            val isNotFound = notFoundAlbumIds.contains(album.mbid)

            LaunchedEffect(album, isImported, isNotFound) {
                imageBitmap.value =
                    if (!isImported && !isNotFound) viewModel.getAlbumArt(album)
                    else null
            }

            ImportableAlbumRow(
                imageBitmap = imageBitmap.value,
                isImported = isImported,
                isNotFound = isNotFound,
                albumTitle = album.name,
                artist = album.artist.name,
                thirdRow = {
                    album.playcount?.also { playCount ->
                        Text(
                            text = stringResource(R.string.play_count, playCount),
                            style = ThouCylinderTheme.typographyExtended.listNormalSubtitleSecondary,
                            maxLines = 1,
                        )
                    }
                },
            )
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImportLastFmHeader(
    modifier: Modifier = Modifier,
    hasPrevious: Boolean,
    hasNext: Boolean,
    offset: Int,
    currentAlbumCount: Int,
    totalAlbumCount: Int,
    progress: ImportProgressData?,
    searchTerm: String,
    selectAllEnabled: Boolean,
    isAllSelected: Boolean,
    importButtonEnabled: Boolean,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSearch: (String) -> Unit,
    onSelectAllClick: (Boolean) -> Unit,
    onImportClick: () -> Unit,
    backendSelection: @Composable () -> Unit,
) {
    var isSearchFocused by rememberSaveable { mutableStateOf(false) }
    val isLandscape = isInLandscapeMode()

    Surface(tonalElevation = 2.dp, color = BottomAppBarDefaults.containerColor) {
        Column(
            modifier = modifier
                .padding(horizontal = 10.dp)
                .padding(bottom = 10.dp, top = if (isLandscape) 10.dp else 0.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ProvideTextStyle(value = ThouCylinderTheme.typographyExtended.listNormalTitle) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = if (isLandscape) 3 else 2,
                ) {
                    backendSelection()

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
                        isTotalAlbumCountExact = false,
                        hasPrevious = hasPrevious,
                        hasNext = hasNext,
                        onPreviousClick = onPreviousClick,
                        onNextClick = onNextClick,
                    )

                    if (isLandscape) {
                        SmallButton(
                            onClick = onImportClick,
                            text = stringResource(R.string.import_str),
                            enabled = importButtonEnabled,
                        )
                    }

                    if (!isLandscape) {
                        SelectAllCheckbox(
                            checked = isAllSelected,
                            enabled = selectAllEnabled,
                            onCheckedChange = onSelectAllClick,
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                    }

                    CompactSearchTextField(
                        value = searchTerm,
                        onSearch = onSearch,
                        onFocusChanged = { isSearchFocused = it.isFocused },
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
                }

                ProgressSection(progress = progress)
            }
        }
    }
}
