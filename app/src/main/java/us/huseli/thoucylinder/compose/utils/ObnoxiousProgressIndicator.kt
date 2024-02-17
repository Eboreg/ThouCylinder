package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import us.huseli.retaintheme.extensions.circular
import us.huseli.retaintheme.ui.theme.RetainBasicColorsDark
import us.huseli.retaintheme.ui.theme.RetainBasicColorsLight
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.stringResource

@Composable
fun ObnoxiousProgressIndicator(
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.loading_scream),
    textStyle: TextStyle = LocalTextStyle.current,
    wiggleDp: Dp = 6.dp,
    tonalElevation: Dp = 5.dp,
    padding: PaddingValues = PaddingValues(wiggleDp),
) {
    val wigglePx = with(LocalDensity.current) { wiggleDp.toPx().toInt() }
    val colors = listOf(RetainBasicColorsDark, RetainBasicColorsLight).map {
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
    val annotateString: (Int) -> AnnotatedString = { colorOffset ->
        AnnotatedString(
            text = text,
            spanStyles = colors.circular(colorOffset, text.length).mapIndexed { index, color ->
                AnnotatedString.Range(SpanStyle(color = color), index, index + 1)
            }
        )
    }

    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.padding(padding).fillMaxWidth(),
    ) {
        var offset by remember { mutableStateOf(IntOffset(0, 0)) }
        var annotatedText by remember(text) { mutableStateOf(annotateString(0)) }

        LaunchedEffect(text) {
            var colorOffset = 0
            while (true) {
                offset = IntOffset((-wigglePx..wigglePx).random(), (-wigglePx..wigglePx).random())
                annotatedText = annotateString(colorOffset)
                if (colorOffset == 0) colorOffset = colors.lastIndex else colorOffset--
                delay(100L)
            }
        }

        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            tonalElevation = tonalElevation,
            modifier = Modifier.absoluteOffset { offset },
        ) {
            Text(
                text = annotatedText,
                style = textStyle,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                softWrap = false,
            )
        }
    }
}
