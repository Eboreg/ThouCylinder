package us.huseli.thoucylinder.compose.modalcover

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.viewmodels.QueueViewModel

@Composable
fun CollapsedProgressBar(viewModel: QueueViewModel) {
    val currentProgress by viewModel.currentProgress.collectAsStateWithLifecycle(0f)

    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), progress = { currentProgress })
}
