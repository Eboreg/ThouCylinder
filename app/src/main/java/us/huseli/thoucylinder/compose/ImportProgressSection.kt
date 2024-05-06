package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.dataclasses.ProgressData
import us.huseli.thoucylinder.umlautify

@Composable
fun ImportProgressSection(progress: ProgressData, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = 5.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = progress.text.umlautify() + " ...",
            style = MaterialTheme.typography.labelLarge,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
        LinearProgressIndicator(
            progress = { progress.progress.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
