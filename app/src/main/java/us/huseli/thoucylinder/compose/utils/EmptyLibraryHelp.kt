package us.huseli.thoucylinder.compose.utils

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import us.huseli.thoucylinder.appendLink
import us.huseli.thoucylinder.dataclasses.callbacks.LocalAppCallbacks
import us.huseli.thoucylinder.umlautify

@Composable
fun EmptyLibraryHelp(modifier: Modifier = Modifier) {
    val callbacks = LocalAppCallbacks.current
    val text = buildAnnotatedString {
        append("You can add music by either ".umlautify())
        appendLink(text = "searching", onClick = callbacks.onGotoSearchClick)
        append(" for it or ")
        appendLink(text = "importing", onClick = callbacks.onGotoImportClick)
        append(" it. In the ")
        appendLink(text = "settings", onClick = callbacks.onGotoSettingsClick)
        append(", you can also configure the app to auto import your local music.")
    }

    Text(text = text, modifier = modifier)
}
