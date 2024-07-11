package us.huseli.thoucylinder.compose.modalcover

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.umlautify

@Composable
fun TitlesColumn(
    state: ModalCoverState,
    title: String,
    artist: String?,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    padding: PaddingValues = PaddingValues(top = 0.dp),
    alpha: Float = 1f,
) {
    val textColor = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha)
    val artistTextSize by remember { derivedStateOf { 14f + (6f * state.expandProgress) } }
    val titleTextSize by remember { derivedStateOf { 16f + (12f * state.expandProgress) } }
    val titlesGap by remember { derivedStateOf { 5.dp * state.expandProgress } }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = horizontalAlignment,
        modifier = modifier.padding(padding),
    ) {
        Text(
            text = title.umlautify(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = FistopyTheme.bodyStyles.primaryBold,
            fontSize = titleTextSize.sp,
            color = textColor,
            modifier = Modifier.basicMarquee(Int.MAX_VALUE, initialDelayMillis = 1000),
        )
        if (artist != null) {
            Text(
                text = artist.umlautify(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = FistopyTheme.bodyStyles.primarySmall,
                fontSize = artistTextSize.sp,
                color = textColor,
                modifier = Modifier
                    .padding(top = titlesGap)
                    .basicMarquee(Int.MAX_VALUE, initialDelayMillis = 1000),
            )
        }
    }
}
