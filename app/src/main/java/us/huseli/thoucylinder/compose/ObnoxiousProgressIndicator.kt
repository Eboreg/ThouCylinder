package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import us.huseli.retaintheme.ui.theme.RetainColorDark
import us.huseli.retaintheme.ui.theme.RetainColorLight
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.circular

@Composable
fun ObnoxiousProgressIndicator(
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.loading_scream),
    textStyle: TextStyle = LocalTextStyle.current,
    wigglePx: Int = 20,
) {
    var rowWidth by remember { mutableIntStateOf(0) }
    val colors = listOf(RetainColorDark, RetainColorLight).map {
        listOf(
            it.Brown,
            it.Blue,
            it.Cerulean,
            it.Gray,
            it.Green,
            it.Orange,
            it.Pink,
            it.Purple,
            it.Red,
            it.Teal,
            it.Yellow,
        )
    }.flatten().shuffled()

    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { size ->
                if (rowWidth != size.width) rowWidth = size.width
            },
    ) {
        var textWidth by remember { mutableIntStateOf(0) }
        var textOffset by remember { mutableStateOf(IntOffset(0, 0)) }
        var annotatedText by remember { mutableStateOf(AnnotatedString(text = text)) }

        LaunchedEffect(Unit) {
            var colorOffset = 0
            while (true) {
                textOffset = IntOffset((-wigglePx..wigglePx).random(), (-wigglePx..wigglePx).random())
                annotatedText = AnnotatedString(
                    text = text,
                    spanStyles = colors.circular(colorOffset, text.length).mapIndexed { index, color ->
                        AnnotatedString.Range(SpanStyle(color = color), index, index + 1)
                    }
                )
                if (colorOffset == 0) colorOffset = colors.lastIndex else colorOffset--
                delay(100L)
            }
        }

        Text(
            text = annotatedText,
            style = textStyle,
            modifier = Modifier
                .onSizeChanged { size ->
                    if (textWidth == 0) textWidth = size.width
                }
                .absoluteOffset { textOffset },
        )
    }
}
