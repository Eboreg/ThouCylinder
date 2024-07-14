package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun IsLoadingProgressIndicator(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(),
    text: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(padding),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (text != null) Text(text = text, style = MaterialTheme.typography.bodySmall)
        LinearProgressIndicator(modifier = Modifier.weight(1f))
    }
}
