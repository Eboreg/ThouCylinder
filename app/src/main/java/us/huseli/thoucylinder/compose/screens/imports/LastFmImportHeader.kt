package us.huseli.thoucylinder.compose.screens.imports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.isInLandscapeMode
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.ImportProgressSection
import us.huseli.thoucylinder.compose.utils.CompactSearchTextField
import us.huseli.thoucylinder.compose.utils.SmallButton
import us.huseli.thoucylinder.dataclasses.ProgressData
import us.huseli.thoucylinder.stringResource


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LastFmImportHeader(
    modifier: Modifier = Modifier,
    hasPrevious: Boolean,
    hasNext: Boolean,
    offset: Int,
    currentAlbumCount: Int,
    totalAlbumCount: Int,
    progress: ProgressData?,
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
                        SelectAllChip(
                            selected = isAllSelected,
                            enabled = selectAllEnabled,
                            onClick = { onSelectAllClick(!isAllSelected) },
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
                        SelectAllChip(
                            selected = isAllSelected,
                            enabled = selectAllEnabled,
                            onClick = { onSelectAllClick(!isAllSelected) },
                            modifier = Modifier.align(Alignment.CenterVertically).padding(start = 10.dp),
                        )
                    }
                }

                ImportProgressSection(progress = progress)
            }
        }
    }
}
