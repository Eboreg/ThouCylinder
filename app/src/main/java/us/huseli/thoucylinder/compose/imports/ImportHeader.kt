package us.huseli.thoucylinder.compose.imports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.isInLandscapeMode
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.compose.ProgressSection
import us.huseli.thoucylinder.compose.utils.CollapsibleToolbar
import us.huseli.thoucylinder.compose.utils.CompactSearchTextField
import us.huseli.thoucylinder.compose.utils.SmallButton
import us.huseli.thoucylinder.compose.utils.Toolbar
import us.huseli.thoucylinder.externalcontent.ImportBackend
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.ExternalAlbumImportViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImportHeader(
    viewModel: ExternalAlbumImportViewModel,
    activeBackend: ImportBackend,
    canImport: Boolean,
    onImportClick: () -> Unit,
    onSelectDirectoryClick: () -> Unit,
    show: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    val isLandscape = isInLandscapeMode()
    var isSearchFocused by rememberSaveable { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val currentAlbumCount by viewModel.currentAlbumCount.collectAsStateWithLifecycle()
    val totalAlbumCount by viewModel.totalItemCount.collectAsStateWithLifecycle()
    val hasNext by viewModel.hasNextPage.collectAsStateWithLifecycle()
    val hasPrevious by viewModel.hasPreviousPage.collectAsStateWithLifecycle()
    val isAllSelected by viewModel.isAllSelected.collectAsStateWithLifecycle()
    val isImportButtonEnabled by viewModel.isImportButtonEnabled.collectAsStateWithLifecycle()
    val isSelectAllEnabled by viewModel.isSelectAllEnabled.collectAsStateWithLifecycle()
    val isTotalAlbumCountExact by viewModel.isTotalAlbumCountExact.collectAsStateWithLifecycle()
    val offset by viewModel.displayOffset.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val searchTerm by viewModel.searchTerm.collectAsStateWithLifecycle()
    val selectedAlbumCount by viewModel.selectedAlbumCount.collectAsStateWithLifecycle()

    LaunchedEffect(activeBackend) {
        viewModel.initBackend()
    }

    ProvideTextStyle(value = FistopyTheme.bodyStyles.primary) {
        CollapsibleToolbar(show = show, modifier = modifier) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = if (isLandscape) 3 else 2,
            ) {
                BackendSelection(
                    activeBackend = activeBackend,
                    onSelect = remember { { viewModel.setBackend(it) } },
                    modifier = if (!isLandscape) Modifier.fillMaxWidth() else Modifier,
                )

                if (canImport) {
                    PaginationSection(
                        currentItemCount = currentAlbumCount,
                        offset = offset,
                        totalItemCount = totalAlbumCount,
                        isTotalItemCountExact = isTotalAlbumCountExact,
                        hasPrevious = hasPrevious,
                        hasNext = hasNext,
                        onPreviousClick = { viewModel.gotoPreviousPage() },
                        onNextClick = { viewModel.gotoNextPage() },
                    )

                    SelectAllChip(
                        selected = isAllSelected,
                        enabled = isSelectAllEnabled,
                        onClick = remember { { viewModel.toggleSelectAll() } },
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }
        }

        Toolbar {
            Row {
                if (canImport) {
                    CompactSearchTextField(
                        value = { searchTerm },
                        onSearch = remember {
                            {
                                viewModel.setSearchTerm(it)
                                keyboardController?.hide()
                            }
                        },
                        onFocusChanged = { isSearchFocused = it.isFocused },
                        placeholderText = stringResource(R.string.search),
                        modifier = Modifier.weight(1f).padding(end = 10.dp),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (activeBackend == ImportBackend.SPOTIFY) {
                        if (BuildConfig.DEBUG && canImport) SmallButton(
                            onClick = remember { { viewModel.unauthorizeSpotify() } },
                            text = "Unauth",
                        )
                    }
                    if (activeBackend == ImportBackend.LOCAL && canImport) SmallButton(
                        onClick = onSelectDirectoryClick,
                        text = stringResource(R.string.select_directory),
                    )
                    if (canImport) SmallButton(
                        onClick = onImportClick,
                        text = when (selectedAlbumCount) {
                            0 -> stringResource(R.string.import_str)
                            else -> stringResource(R.string.import_x, selectedAlbumCount)
                        },
                        enabled = isImportButtonEnabled,
                    )
                }
            }

            ProgressSection(progress = progress)
        }
    }
}
