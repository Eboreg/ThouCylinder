package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R

@Composable
fun AlbumSmallIcons(isLocal: Boolean, isOnYoutube: Boolean, modifier: Modifier = Modifier) {
    if (isLocal) {
        Icon(
            painter = painterResource(R.drawable.hard_drive_filled),
            contentDescription = null,
            modifier = modifier.padding(vertical = 2.dp).width(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (isOnYoutube) {
        Icon(
            painter = painterResource(R.drawable.youtube),
            contentDescription = null,
            modifier = modifier.padding(vertical = 2.dp).width(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
