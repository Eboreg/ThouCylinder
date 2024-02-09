package us.huseli.thoucylinder.compose.modalcover

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import us.huseli.thoucylinder.ThouCylinderTheme

@Suppress("AnimateAsStateLabel")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TitlesColumn(modifier: Modifier = Modifier, isExpanded: Boolean, title: String, artist: String?, alpha: Float) {
    val intAnimationSpec = tween<Int>(150)
    val artistTextSize by animateIntAsState(if (isExpanded) 18 else 14, intAnimationSpec)
    val titleTextSize by animateIntAsState(if (isExpanded) 24 else 16, intAnimationSpec)
    val textColor = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha)

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = if (isExpanded) Alignment.CenterHorizontally else Alignment.Start,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = ThouCylinderTheme.typographyExtended.listNormalHeader,
            fontSize = titleTextSize.sp,
            color = textColor,
            modifier = Modifier.basicMarquee(Int.MAX_VALUE),
        )
        if (artist != null) {
            Text(
                text = artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = ThouCylinderTheme.typographyExtended.listNormalSubtitle,
                fontSize = artistTextSize.sp,
                modifier = Modifier.basicMarquee(Int.MAX_VALUE),
                color = textColor,
            )
        }
    }
}