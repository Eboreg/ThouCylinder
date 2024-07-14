package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.dataclasses.callbacks.LocalAppCallbacks
import us.huseli.thoucylinder.stringResource

@Composable
fun BasicHeader(title: String, trailingContent: @Composable () -> Unit = {}) {
    val callbacks = LocalAppCallbacks.current

    Toolbar(padding = PaddingValues(horizontal = 10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            LargerIconButton(
                icon = Icons.AutoMirrored.Sharp.ArrowBack,
                onClick = callbacks.onBackClick,
                description = stringResource(R.string.go_back),
            )
            Text(
                text = title,
                style = if (title.length > 30) FistopyTheme.typography.titleMedium else FistopyTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = if (title.length > 30) 20.sp else 24.sp,
            )
            trailingContent()
        }
    }
}
