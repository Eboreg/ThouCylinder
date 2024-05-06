package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.InterpreterMode
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.extensions.capitalized
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.compose.utils.SmallOutlinedButton
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.RecommendationsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecommendationsScreen(
    modifier: Modifier = Modifier,
    viewModel: RecommendationsViewModel = hiltViewModel(),
) {
    val spotifyRelatedArtistMatches by viewModel.spotifyRelatedArtistMatches.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 10.dp),
        ) {
            SmallOutlinedButton(
                onClick = { viewModel.getLocalRelatedArtists() },
                content = { Text("Get local related artists") },
            )
            SmallOutlinedButton(
                onClick = { viewModel.getLastFmRelatedArtists() },
                content = { Text("Get Last.fm related artists") },
            )
        }

        ItemList(things = spotifyRelatedArtistMatches) { _, match ->
            var thumbnail by remember { mutableStateOf<ImageBitmap?>(null) }

            LaunchedEffect(match.spotifyArtist.images) {
                thumbnail = viewModel.getArtistThumbnail(match.spotifyArtist, context)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Thumbnail(
                    imageBitmap = { thumbnail },
                    shape = MaterialTheme.shapes.extraSmall,
                    placeholderIcon = Icons.Sharp.InterpreterMode,
                )
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Text(
                        text = match.spotifyArtist.name.umlautify(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = ThouCylinderTheme.typographyExtended.listNormalHeader,
                    )
                    Text(
                        text = stringResource(R.string.related_to_x, match.artists.joinToString(", ")),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = ThouCylinderTheme.typographyExtended.listExtraSmallTitleSecondary,
                    )
                    if (match.spotifyArtist.genres.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            match.spotifyArtist.genres.forEach { genre ->
                                var visible by remember { mutableStateOf(false) }

                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    content = { Text(text = genre.capitalized().umlautify(), maxLines = 1) },
                                    modifier = Modifier.wrapContentWidth(unbounded = true).onPlaced { coords ->
                                        coords.parentCoordinates?.also { parentCoords ->
                                            visible = parentCoords.size.width >= coords.size.width
                                        }
                                    }.alpha(if (visible) 1f else 0f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
