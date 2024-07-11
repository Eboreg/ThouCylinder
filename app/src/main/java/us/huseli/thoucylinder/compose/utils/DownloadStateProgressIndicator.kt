package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.AbstractDownloadUiState

@Composable
fun DownloadStateProgressIndicator(
    downloadState: AbstractDownloadUiState,
    modifier: Modifier = Modifier,
    height: Dp = 3.dp,
) {
    if (downloadState.isActive) LinearProgressIndicator(
        progress = { downloadState.progress },
        modifier = modifier.fillMaxWidth().height(height),
        drawStopIndicator = {},
    ) else Spacer(modifier = Modifier.height(height))
}
