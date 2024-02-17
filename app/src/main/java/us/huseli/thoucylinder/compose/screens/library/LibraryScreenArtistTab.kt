package us.huseli.thoucylinder.compose.screens.library

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.ArtistGrid
import us.huseli.thoucylinder.compose.ArtistList
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.ArtistListViewModel

@Composable
fun LibraryScreenArtistTab(
    displayType: DisplayType,
    isImporting: Boolean,
    viewModel: ArtistListViewModel = hiltViewModel(),
    onArtistClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val artistPojos by viewModel.artistPojos.collectAsStateWithLifecycle(emptyList())
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val progressIndicatorText =
        if (isImporting) stringResource(R.string.importing_local_artists)
        else if (isLoading) stringResource(R.string.loading_artists)
        else null
    val onEmpty: @Composable () -> Unit = {
        if (!isImporting && !isLoading) Text(
            stringResource(R.string.no_artists_found),
            modifier = Modifier.padding(10.dp),
        )
    }

    when (displayType) {
        DisplayType.LIST -> ArtistList(
            onArtistClick = onArtistClick,
            progressIndicatorText = progressIndicatorText,
            artistPojos = artistPojos,
            onEmpty = onEmpty,
            imageFlow = { viewModel.flowArtistImage(it, context) },
        )
        DisplayType.GRID -> ArtistGrid(
            onArtistClick = onArtistClick,
            artistPojos = artistPojos,
            progressIndicatorText = progressIndicatorText,
            onEmpty = onEmpty,
            imageFlow = { viewModel.flowArtistImage(it, context) },
        )
    }
}
