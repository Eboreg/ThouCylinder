package us.huseli.thoucylinder.compose.screens.imports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.isInLandscapeMode
import us.huseli.thoucylinder.AuthorizationStatus
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.ImportProgressSection
import us.huseli.thoucylinder.compose.utils.CompactSearchTextField
import us.huseli.thoucylinder.compose.utils.SmallButton
import us.huseli.thoucylinder.dataclasses.ProgressData
import us.huseli.thoucylinder.stringResource


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpotifyImportHeader(
    modifier: Modifier = Modifier,
    authorizationStatus: AuthorizationStatus,
    hasPrevious: Boolean,
    hasNext: Boolean,
    importButtonEnabled: Boolean,
    selectAllEnabled: Boolean,
    offset: Int,
    currentAlbumCount: Int,
    totalAlbumCount: Int?,
    isTotalAlbumCountExact: Boolean,
    isAllSelected: Boolean,
    progress: ProgressData?,
    searchTerm: String,
    onImportClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSelectAllClick: (Boolean) -> Unit,
    onSearch: (String) -> Unit,
    onAuthorizeClick: () -> Unit,
    onUnauthorizeClick: () -> Unit,
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

                    if (authorizationStatus == AuthorizationStatus.AUTHORIZED) {
                        if (!isLandscape) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (BuildConfig.DEBUG) SmallButton(
                                    onClick = onUnauthorizeClick,
                                    text = "Unauth",
                                )
                                SmallButton(
                                    onClick = onImportClick,
                                    text = stringResource(R.string.import_str),
                                    enabled = importButtonEnabled,
                                )
                            }
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
                            SelectAllChip(
                                selected = isAllSelected,
                                enabled = selectAllEnabled,
                                onClick = { onSelectAllClick(!isAllSelected) },
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
                            SelectAllChip(
                                selected = isAllSelected,
                                enabled = selectAllEnabled,
                                onClick = { onSelectAllClick(!isAllSelected) },
                                modifier = Modifier.align(Alignment.CenterVertically).padding(start = 10.dp),
                            )
                        }
                    } else if (authorizationStatus != AuthorizationStatus.UNKNOWN) {
                        SmallButton(onClick = onAuthorizeClick, text = stringResource(R.string.authorize))
                    }
                }

                ImportProgressSection(progress = progress)
            }
        }
    }
}
