package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.umlautify

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlbumBadges(
    tags: List<String>,
    year: String?,
    modifier: Modifier = Modifier,
) {
    if (tags.isNotEmpty() || year != null) {
        FlowRow(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = modifier) {
            if (year != null) {
                Box(modifier = Modifier.padding(horizontal = 2.5.dp)) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        content = { Text(text = year) },
                    )
                }
            }
            tags.forEach { tag ->
                Box(modifier = Modifier.padding(horizontal = 2.5.dp)) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        content = { Text(text = tag.umlautify()) },
                    )
                }
            }
        }
    }
}
