package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AlbumBadges(
    genres: List<String>,
    styles: List<String>,
    modifier: Modifier = Modifier,
) {
    if (genres.isNotEmpty() || styles.isNotEmpty()) {
        FlowRow(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = modifier.padding(vertical = 10.dp),
        ) {
            genres.forEach { genre ->
                Box(modifier = Modifier.padding(horizontal = 2.5.dp)) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        content = { Text(text = genre) },
                    )
                }
            }
            styles.forEach { style ->
                Box(modifier = Modifier.padding(horizontal = 2.5.dp)) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        content = { Text(text = style) },
                    )
                }
            }
        }
    }
}
