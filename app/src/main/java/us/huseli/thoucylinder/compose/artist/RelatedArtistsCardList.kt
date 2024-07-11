package us.huseli.thoucylinder.compose.artist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.compose.utils.HorizontalCardList
import us.huseli.thoucylinder.dataclasses.artist.UnsavedArtist
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify

@Composable
fun RelatedArtistsCardList(
    relatedArtists: ImmutableList<UnsavedArtist>,
    onClick: (UnsavedArtist) -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.related_artists),
            style = FistopyTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 5.dp),
        )
        HorizontalCardList(
            things = { relatedArtists },
            key = { it.spotifyId ?: it.name },
            thumbnailModel = { it.image },
            text = { relatedArtist ->
                Text(
                    text = relatedArtist.name.umlautify(),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(5.dp),
                    style = FistopyTheme.bodyStyles.primarySmall.copy(lineHeight = 18.sp),
                )
            },
            onClick = onClick,
        )
    }
}