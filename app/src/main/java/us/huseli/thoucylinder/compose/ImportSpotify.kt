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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.extensions.sensibleFormat
import us.huseli.retaintheme.isInLandscapeMode
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.SpotifyOAuth2
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.screens.PaginationSection
import us.huseli.thoucylinder.compose.screens.ProgressSection
import us.huseli.thoucylinder.compose.screens.SelectAllCheckbox
import us.huseli.thoucylinder.compose.utils.CompactSearchTextField
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.compose.utils.ObnoxiousProgressIndicator
import us.huseli.thoucylinder.compose.utils.SmallButton
import us.huseli.thoucylinder.dataclasses.ImportProgressData
import us.huseli.thoucylinder.pluralStringResource
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.SpotifyImportViewModel
import java.util.UUID
import kotlin.math.max

@Composable
fun ImportSpotify(
    modifier: Modifier = Modifier,
    viewModel: SpotifyImportViewModel = hiltViewModel(),
    listState: LazyListState = rememberLazyListState(),
    onGotoLibraryClick: () -> Unit,
    onGotoAlbumClick: (UUID) -> Unit,
    backendSelection: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val filteredAlbumCount by viewModel.filteredAlbumCount.collectAsStateWithLifecycle(null)
    val hasNext by viewModel.hasNext.collectAsStateWithLifecycle(false)
    val importedAlbumIds by viewModel.importedAlbumIds.collectAsStateWithLifecycle()
    val isAllSelected by viewModel.isAllSelected.collectAsStateWithLifecycle(false)
    val authorizationStatus by viewModel.authorizationStatus.collectAsStateWithLifecycle(
        SpotifyOAuth2.AuthorizationStatus.UNKNOWN
    )
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val isAlbumCountExact by viewModel.isAlbumCountExact.collectAsStateWithLifecycle(false)
    val nextAlbumIdx by viewModel.nextAlbumIdx.collectAsStateWithLifecycle()
    val notFoundAlbumIds by viewModel.notFoundAlbumIds.collectAsStateWithLifecycle()
    val offset by viewModel.localOffset.collectAsStateWithLifecycle(0)
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val searchTerm by viewModel.searchTerm.collectAsStateWithLifecycle()
    val selectedUserAlbums by viewModel.selectedSpotifyAlbums.collectAsStateWithLifecycle()
    val totalAlbumCount by viewModel.totalAlbumCount.collectAsStateWithLifecycle()
    val spotifyAlbums by viewModel.offsetSpotifyAlbums.collectAsStateWithLifecycle(emptyList())

    LaunchedEffect(authorizationStatus) {
        // if (authorizationStatus == SpotifyRepository.AuthorizationStatus.AUTHORIZED) viewModel.setOffset(0)
        viewModel.setOffset(0)
    }

    LaunchedEffect(spotifyAlbums.firstOrNull()) {
        if (spotifyAlbums.isNotEmpty()) listState.scrollToItem(0)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        ImportSpotifyHeader(
            authorizationStatus = authorizationStatus,
            hasPrevious = offset > 0,
            hasNext = hasNext,
            importButtonEnabled = progress == null && selectedUserAlbums.isNotEmpty(),
            selectAllEnabled = spotifyAlbums.isNotEmpty(),
            offset = offset,
            currentAlbumCount = spotifyAlbums.size,
            totalAlbumCount = filteredAlbumCount,
            isTotalAlbumCountExact = isAlbumCountExact,
            isAllSelected = isAllSelected,
            progress = progress,
            searchTerm = searchTerm,
            onImportClick = {
                viewModel.importSelectedAlbums { importedIds, notFoundCount ->
                    val strings = mutableListOf<String>()
                    if (importedIds.isNotEmpty()) {
                        strings.add(
                            context.resources.getQuantityString(
                                R.plurals.x_albums_imported,
                                importedIds.size,
                                importedIds.size,
                            ).umlautify()
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
                    if (strings.isNotEmpty()) {
                        val actionLabel =
                            if (importedIds.size == 1) context.getString(R.string.go_to_album).umlautify()
                            else context.getString(R.string.go_to_library).umlautify()

                        SnackbarEngine.addInfo(
                            message = strings.joinToString(" ").umlautify(),
                            actionLabel = actionLabel,
                            onActionPerformed = {
                                if (importedIds.size == 1) onGotoAlbumClick(importedIds[0])
                                else onGotoLibraryClick()
                            },
                        )
                    }
                }
            },
            onPreviousClick = { viewModel.setOffset(max(offset - 50, 0)) },
            onNextClick = { viewModel.setOffset(offset + 50) },
            onSelectAllClick = { viewModel.setSelectAll(it) },
            onSearch = { viewModel.setSearchTerm(it) },
            onAuthorizeClick = { uriHandler.openUri(viewModel.getAuthUrl()) },
            backendSelection = backendSelection,
        )

        if (spotifyAlbums.isEmpty()) {
            if (authorizationStatus == SpotifyOAuth2.AuthorizationStatus.UNAUTHORIZED) {
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
            things = spotifyAlbums,
            cardHeight = 60.dp,
            key = { _, album -> album.id },
            gap = 5.dp,
            isSelected = { selectedUserAlbums.contains(it) },
            onClick = { _, album ->
                val importedAlbumId = importedAlbumIds[album.id]

                if (importedAlbumId != null) onGotoAlbumClick(importedAlbumId)
                else viewModel.toggleSelected(album)
            },
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
        ) { _, album ->
            val imageBitmap = remember(album) { mutableStateOf<ImageBitmap?>(null) }
            val isImported = importedAlbumIds.containsKey(album.id)
            val isNotFound = notFoundAlbumIds.contains(album.id)

            LaunchedEffect(album, isImported, isNotFound) {
                if (!isImported && !isNotFound) imageBitmap.value = viewModel.getThumbnail(album)
                else imageBitmap.value = null
            }

            ImportableAlbumRow(
                imageBitmap = imageBitmap.value,
                isImported = isImported,
                isNotFound = isNotFound,
                albumTitle = album.name,
                artist = album.artist,
                thirdRow = {
                    val count = album.tracks.items.size

                    Text(
                        text = pluralStringResource(R.plurals.x_tracks, count, count) +
                            " • ${album.year} • ${album.duration.sensibleFormat()}",
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
    authorizationStatus: SpotifyOAuth2.AuthorizationStatus,
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

                    if (authorizationStatus == SpotifyOAuth2.AuthorizationStatus.AUTHORIZED) {
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
                    } else if (authorizationStatus != SpotifyOAuth2.AuthorizationStatus.UNKNOWN) {
                        SmallButton(onClick = onAuthorizeClick, text = stringResource(R.string.authorize))
                    }
                }

                ProgressSection(progress = progress)
            }
        }
    }
}
