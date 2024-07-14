package us.huseli.thoucylinder.compose.artist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.InterpreterMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.compose.utils.HorizontalCardList
import us.huseli.thoucylinder.compose.utils.SmallOutlinedButton
import us.huseli.thoucylinder.dataclasses.artist.UnsavedArtist
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify

@Composable
fun RelatedArtistsCardList(
    relatedArtists: ImmutableList<UnsavedArtist>,
    onClick: (UnsavedArtist) -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.related_artists),
                style = FistopyTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 5.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(R.string.courtesy_of_spotify), style = MaterialTheme.typography.bodyMedium)
                Icon(
                    painter = painterResource(R.drawable.spotify),
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
        HorizontalCardList(
            things = { relatedArtists },
            key = { it.spotifyId ?: it.name },
            thumbnailModel = { it.image },
            placeHolderIcon = Icons.Sharp.InterpreterMode,
            cardHeight = 185.dp,
            text = { relatedArtist ->
                Column(
                    modifier = Modifier.fillMaxWidth().padding(5.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = relatedArtist.name.umlautify(),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(),
                            style = FistopyTheme.bodyStyles.primarySmall.copy(lineHeight = 18.sp),
                        )
                    }
                    relatedArtist.spotifyWebUrl?.also { url ->
                        SmallOutlinedButton(
                            onClick = { uriHandler.openUri(url) },
                            text = stringResource(R.string.on_spotify),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            },
            onClick = onClick,
        )
    }
}
