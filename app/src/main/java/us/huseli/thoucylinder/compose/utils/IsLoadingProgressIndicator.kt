package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun IsLoadingProgressIndicator(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(10.dp),
) {
    LinearProgressIndicator(modifier = modifier.fillMaxWidth().padding(padding))
}
