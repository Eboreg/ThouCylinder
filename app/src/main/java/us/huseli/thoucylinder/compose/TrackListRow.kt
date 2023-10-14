package us.huseli.thoucylinder.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.sensibleFormat
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.DownloadProgress
import us.huseli.thoucylinder.dataclasses.TrackMetadata
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackListRow(
    track: Track,
    metadata: TrackMetadata?,
    showArtist: Boolean,
    onDownloadClick: () -> Unit,
    onPlayClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onToggleSelected: () -> Unit,
    thumbnail: ImageBitmap?,
    modifier: Modifier = Modifier,
    album: Album? = null,
    isSelected: Boolean = false,
    selectOnShortClick: Boolean = false,
    containerColor: Color? = null,
    downloadProgress: DownloadProgress? = null,
    onArtistClick: ((String) -> Unit)? = null,
    onAlbumClick: ((UUID) -> Unit)? = null,
    onEnqueueNextClick: () -> Unit,
) {
    val artist = track.artist ?: album?.artist

    Card(
        colors = CardDefaults.outlinedCardColors(
            containerColor = containerColor
                ?: if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        ),
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier.fillMaxWidth().height(50.dp).combinedClickable(
            onClick = { if (selectOnShortClick) onToggleSelected() else onPlayClick() },
            onLongClick = onToggleSelected,
        ),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Thumbnail(
                image = thumbnail,
                shape = MaterialTheme.shapes.extraSmall,
                placeholder = {
                    Image(
                        imageVector = Icons.Sharp.MusicNote,
                        contentDescription = null,
                    )
                },
                borderWidth = if (isSelected) 0.dp else 1.dp,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxHeight().padding(vertical = 5.dp),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = track.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = ThouCylinderTheme.typographyExtended.listSmallHeader,
                    )
                    if (artist != null && showArtist) {
                        Text(
                            text = artist,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                        )
                    }
                }

                metadata?.let {
                    Text(text = it.duration.sensibleFormat(), modifier = Modifier.padding(start = 5.dp))
                }

                TrackContextMenuWithButton(
                    track = track,
                    album = album,
                    metadata = metadata,
                    onDownloadClick = onDownloadClick,
                    modifier = Modifier.padding(start = 10.dp).width(30.dp),
                    onAlbumClick = onAlbumClick,
                    onArtistClick = onArtistClick,
                    onAddToPlaylistClick = onAddToPlaylistClick,
                    onEnqueueNextClick = onEnqueueNextClick,
                )
            }
        }

        if (downloadProgress != null) {
            val statusText = stringResource(downloadProgress.status.stringId)

            Column(modifier = Modifier.padding(bottom = 5.dp)) {
                Text(text = "$statusText …")
                LinearProgressIndicator(
                    progress = downloadProgress.progress.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
