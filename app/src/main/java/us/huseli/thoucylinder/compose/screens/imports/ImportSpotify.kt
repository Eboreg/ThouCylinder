package us.huseli.thoucylinder.compose.screens.imports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.extensions.sensibleFormat
import us.huseli.thoucylinder.AuthorizationStatus
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.pluralStringResource
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.SpotifyImportViewModel
import java.util.UUID
import kotlin.math.max

@Composable
fun ImportSpotify(
    viewModel: SpotifyImportViewModel = hiltViewModel(),
    listState: LazyListState = rememberLazyListState(),
    onGotoLibraryClick: () -> Unit,
    onGotoAlbumClick: (UUID) -> Unit,
    backendSelection: @Composable () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val authorizationStatus by viewModel.authorizationStatus.collectAsStateWithLifecycle(
        AuthorizationStatus.UNKNOWN
    )
    val filteredAlbumCount by viewModel.filteredAlbumCount.collectAsStateWithLifecycle(null)
    val isAlbumCountExact by viewModel.isAlbumCountExact.collectAsStateWithLifecycle(false)

    val externalAlbums by viewModel.offsetExternalAlbums.collectAsStateWithLifecycle(emptyList())
    val hasNext by viewModel.hasNext.collectAsStateWithLifecycle(false)
    val isAllSelected by viewModel.isAllSelected.collectAsStateWithLifecycle(false)
    val offset by viewModel.localOffset.collectAsStateWithLifecycle(0)
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val searchTerm by viewModel.searchTerm.collectAsStateWithLifecycle()
    val selectedExternalAlbumIds by viewModel.selectedExternalAlbumIds.collectAsStateWithLifecycle()

    var importMethodDialogOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(authorizationStatus) {
        viewModel.setOffset(0)
    }

    if (importMethodDialogOpen) {
        ImportMethodDialog(
            title = stringResource(R.string.import_from_spotify),
            viewModel = viewModel,
            onDismissRequest = { importMethodDialogOpen = false },
            onGotoLibraryClick = onGotoLibraryClick,
            onGotoAlbumClick = onGotoAlbumClick,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.import_method_description_1))
                    Text(stringResource(R.string.import_method_description_2_spotify))
                }
            },
        )
    }

    ImportSpotifyHeader(
        authorizationStatus = authorizationStatus,
        hasPrevious = offset > 0,
        hasNext = hasNext,
        importButtonEnabled = progress == null && selectedExternalAlbumIds.isNotEmpty(),
        selectAllEnabled = externalAlbums.isNotEmpty(),
        offset = offset,
        currentAlbumCount = externalAlbums.size,
        totalAlbumCount = filteredAlbumCount,
        isTotalAlbumCountExact = isAlbumCountExact,
        isAllSelected = isAllSelected,
        progress = progress,
        searchTerm = searchTerm,
        onImportClick = { importMethodDialogOpen = true },
        onPreviousClick = { viewModel.setOffset(max(offset - 50, 0)) },
        onNextClick = { viewModel.setOffset(offset + 50) },
        onSelectAllClick = { viewModel.setSelectAll(it) },
        onSearch = { viewModel.setSearchTerm(it) },
        onAuthorizeClick = { uriHandler.openUri(viewModel.getAuthUrl()) },
        backendSelection = backendSelection,
    )

    if (externalAlbums.isEmpty()) {
        if (authorizationStatus == AuthorizationStatus.UNAUTHORIZED) {
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

    ImportItemList(
        viewModel = viewModel,
        onGotoAlbumClick = onGotoAlbumClick,
        albumThirdRow = { album ->
            val count = album.tracks.items.size

            Text(
                text = pluralStringResource(R.plurals.x_tracks, count, count) +
                    " • ${album.year} • ${album.duration.sensibleFormat()}",
                style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
            )
        },
        listState = listState,
        selectedExternalAlbumIds = selectedExternalAlbumIds,
        externalAlbums = externalAlbums,
    )
}
