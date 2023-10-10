package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.LoadStatus
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.RoundedIconBlock
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.dataclasses.entities.Style

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LargeAlbumArtSection(
    albumArt: ImageBitmap?,
    loadStatus: LoadStatus?,
    isOnYoutube: Boolean,
    isLocal: Boolean,
    genres: List<Genre>?,
    styles: List<Style>?,
    onBackClick: () -> Unit,
) {
    AlbumArt(
        image = albumArt,
        loadStatus = loadStatus,
        modifier = Modifier.fillMaxHeight(),
        topContent = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(5.dp),
            ) {
                FilledTonalIconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Sharp.ArrowBack, stringResource(R.string.go_back))
                }
                Row(modifier = Modifier.padding(5.dp)) {
                    if (isOnYoutube || isLocal) {
                        RoundedIconBlock {
                            if (isOnYoutube) {
                                Icon(
                                    painterResource(R.drawable.youtube),
                                    stringResource(R.string.youtube_playlist),
                                    modifier = Modifier.fillMaxHeight(),
                                )
                            }
                            if (isLocal) {
                                Icon(
                                    painterResource(R.drawable.hard_drive),
                                    stringResource(R.string.stored_locally),
                                    modifier = Modifier.fillMaxHeight(),
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomContent = {
            Column(modifier = Modifier.fillMaxWidth().padding(5.dp)) {
                genres?.takeIf { it.isNotEmpty() }?.let { genres ->
                    FlowRow(
                        modifier = Modifier
                            .padding(start = 5.dp, end = 5.dp, bottom = 5.dp)
                            .align(Alignment.CenterHorizontally),
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        genres.forEach { genre ->
                            Box(modifier = Modifier.padding(horizontal = 2.5.dp)) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    content = { Text(text = genre.genreName) },
                                )
                            }
                        }
                    }
                }
                styles?.takeIf { it.isNotEmpty() }?.let { styles ->
                    FlowRow(
                        modifier = Modifier
                            .padding(start = 5.dp, end = 5.dp, bottom = 5.dp)
                            .align(Alignment.CenterHorizontally),
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        styles.forEach { style ->
                            Box(modifier = Modifier.padding(horizontal = 2.5.dp)) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    content = { Text(text = style.styleName) },
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}