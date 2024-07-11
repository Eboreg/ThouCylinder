package us.huseli.thoucylinder

import android.content.Context
import android.graphics.Typeface
import android.text.style.StyleSpan
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.core.text.toSpanned

fun annotateStyledText(input: CharSequence): AnnotatedString {
    val spanned = input.toSpanned()
    val rawString = spanned.toString()
    val styleSpans = spanned.getSpans<StyleSpan>(0, input.length, StyleSpan::class.java)
        ?.filterNotNull() ?: emptyList()
    var lastEnd = 0

    return buildAnnotatedString {
        for (span in styleSpans) {
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            val spanText = spanned.substring(start, end)
            val spanStyle = when (span.style) {
                Typeface.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                Typeface.BOLD_ITALIC -> SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
                Typeface.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                else -> SpanStyle()
            }

            if (start > lastEnd) append(rawString.substring(lastEnd, start).umlautify())
            withStyle(spanStyle) { append(spanText.umlautify()) }
            lastEnd = end
        }
        if (lastEnd < rawString.length) append(rawString.substring(lastEnd).umlautify())
    }
}

fun Context.getAnnotatedString(@StringRes resId: Int) = getText(resId).annotate()

fun CharSequence.annotate() = annotateStyledText(this)

@Composable
@ReadOnlyComposable
fun annotatedStringResource(@StringRes resId: Int): AnnotatedString = LocalContext.current.getAnnotatedString(resId)
