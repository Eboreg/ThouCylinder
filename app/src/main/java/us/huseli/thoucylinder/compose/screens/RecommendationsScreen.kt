package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.RecommendationsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecommendationsScreen(
    modifier: Modifier = Modifier,
    viewModel: RecommendationsViewModel = hiltViewModel(),
) {
    val spotifyRelatedArtistMatches by viewModel.spotifyRelatedArtistMatches.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp)) {
        Button(
            onClick = { viewModel.getSpotifyRelatedArtists() },
            content = { Text("Get Spotify related artists") },
        )

        spotifyRelatedArtistMatches.forEach { (sourceArtists, spotifyArtist, score) ->
            Text(
                text = "${spotifyArtist.name} - popularity ${spotifyArtist.popularity} - followers ${spotifyArtist.followers.total} - score $score",
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(text = sourceArtists.joinToString(", "), style = MaterialTheme.typography.bodySmall)

            if (spotifyArtist.genres.isNotEmpty()) {
                FlowRow(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                ) {
                    spotifyArtist.genres.forEach { genre ->
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            content = { Text(text = genre.umlautify()) },
                        )
                    }
                }
            }
        }
    }
}
