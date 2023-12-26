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
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo

@Composable
fun AlbumSmallIcons(pojo: AbstractAlbumPojo, modifier: Modifier = Modifier) {
    if (pojo.album.isLocal) {
        Icon(
            painter = painterResource(R.drawable.hard_drive_filled),
            contentDescription = null,
            modifier = modifier.padding(vertical = 2.dp).width(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (pojo.album.isOnYoutube) {
        Icon(
            painter = painterResource(R.drawable.youtube),
            contentDescription = null,
            modifier = modifier.padding(vertical = 2.dp).width(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (pojo.isOnSpotify) {
        Icon(
            painter = painterResource(R.drawable.spotify),
            contentDescription = null,
            modifier = modifier.padding(vertical = 2.dp).width(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
