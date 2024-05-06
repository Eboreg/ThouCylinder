package us.huseli.thoucylinder.compose.screens.imports

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.isInLandscapeMode
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.ImportProgressSection
import us.huseli.thoucylinder.compose.screens.BackendSelection
import us.huseli.thoucylinder.compose.screens.ImportBackend
import us.huseli.thoucylinder.compose.utils.CollapsibleToolbar
import us.huseli.thoucylinder.compose.utils.CompactSearchTextField
import us.huseli.thoucylinder.compose.utils.SmallButton
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.ExternalAlbumImportViewModel
import kotlin.math.max

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImportHeader(
    viewModel: ExternalAlbumImportViewModel,
    activeBackend: ImportBackend,
    canImport: Boolean,
    onImportClick: () -> Unit,
    show: () -> Boolean,
) {
    val context = LocalContext.current
    val isLandscape = isInLandscapeMode()
    val uriHandler = LocalUriHandler.current

    val currentAlbumCount by viewModel.currentAlbumCount.collectAsStateWithLifecycle()
    val filteredAlbumCount by viewModel.filteredAlbumCount.collectAsStateWithLifecycle()
    val hasNext by viewModel.hasNext.collectAsStateWithLifecycle()
    val hasPrevious by viewModel.hasPrevious.collectAsStateWithLifecycle()
    val isAllSelected by viewModel.isAllSelected.collectAsStateWithLifecycle()
    val isImportButtonEnabled by viewModel.isImportButtonEnabled.collectAsStateWithLifecycle()
    val isSelectAllEnabled by viewModel.isSelectAllEnabled.collectAsStateWithLifecycle()
    val isTotalAlbumCountExact by viewModel.isTotalAlbumCountExact.collectAsStateWithLifecycle()
    val offset by viewModel.displayOffset.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val searchTerm by viewModel.searchTerm.collectAsStateWithLifecycle()

    var isSearchFocused by rememberSaveable { mutableStateOf(false) }
    val selectDirlauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.setLocalImportUri(uri)
        }
    }

    CollapsibleToolbar(show = show) {
        ProvideTextStyle(value = ThouCylinderTheme.typographyExtended.listNormalTitle) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = if (isLandscape) 3 else 2,
            ) {
                BackendSelection(
                    activeBackend = activeBackend,
                    onSelect = remember { { viewModel.setBackend(it) } },
                    modifier = Modifier.weight(1f),
                )

                if (canImport) {
                    PaginationSection(
                        currentAlbumCount = currentAlbumCount,
                        offset = offset,
                        totalAlbumCount = filteredAlbumCount,
                        isTotalAlbumCountExact = isTotalAlbumCountExact,
                        hasPrevious = hasPrevious,
                        hasNext = hasNext,
                        onPreviousClick = remember(offset) { { viewModel.setOffset(max(offset - 50, 0)) } },
                        onNextClick = remember(offset) { { viewModel.setOffset(offset + 50) } },
                    )

                    SelectAllChip(
                        selected = isAllSelected,
                        enabled = isSelectAllEnabled,
                        onClick = remember { { viewModel.toggleSelectAll() } },
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }

                if (canImport) CompactSearchTextField(
                    value = searchTerm,
                    onSearch = remember { { viewModel.setSearchTerm(it) } },
                    onFocusChanged = { isSearchFocused = it.isFocused },
                    placeholderText = stringResource(R.string.search),
                    modifier = Modifier.weight(1f).padding(end = 10.dp),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (activeBackend == ImportBackend.SPOTIFY) {
                        if (!canImport) SmallButton(
                            onClick = remember { { uriHandler.openUri(viewModel.getSpotifyAuthUrl()) } },
                            text = stringResource(R.string.authorize)
                        )
                        if (BuildConfig.DEBUG && canImport) SmallButton(
                            onClick = remember { { viewModel.unauthorizeSpotify() } },
                            text = "Unauth",
                        )
                    } else if (activeBackend == ImportBackend.LOCAL) SmallButton(
                        onClick = { selectDirlauncher.launch(null) },
                        text = stringResource(R.string.select_directory),
                    )
                    if (canImport) SmallButton(
                        onClick = onImportClick,
                        text = stringResource(R.string.import_str),
                        enabled = isImportButtonEnabled,
                    )
                }
            }

            if (progress.isActive) ImportProgressSection(progress = progress)
        }
    }
}
