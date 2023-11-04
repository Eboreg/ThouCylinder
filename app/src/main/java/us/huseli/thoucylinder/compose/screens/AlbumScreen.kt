package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.compose.AlbumBadges
import us.huseli.thoucylinder.compose.AlbumButtons
import us.huseli.thoucylinder.compose.AlbumTrackRow
import us.huseli.thoucylinder.compose.AlbumTrackRowData
import us.huseli.thoucylinder.compose.SelectedTracksButtons
import us.huseli.thoucylinder.compose.utils.LargeIconBadge
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.viewmodels.AlbumViewModel

@Composable
fun AlbumScreen(
    modifier: Modifier = Modifier,
    viewModel: AlbumViewModel = hiltViewModel(),
    appCallbacks: AppCallbacks,
) {
    val albumArt by viewModel.albumArt.collectAsStateWithLifecycle(null)
    val albumPojo by viewModel.albumPojo.collectAsStateWithLifecycle(null)
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle(null)
    val trackDownloadProgressMap by viewModel.trackDownloadProgressMap.collectAsStateWithLifecycle()
    val selectedTracks by viewModel.selectedTracks.collectAsStateWithLifecycle()
    val context = LocalContext.current

    albumPojo?.let { pojo ->
        Column {
            SelectedTracksButtons(
                trackCount = selectedTracks.size,
                callbacks = TrackSelectionCallbacks(
                    onAddToPlaylistClick = { appCallbacks.onAddToPlaylistClick(Selection(trackPojos = selectedTracks)) },
                    onPlayClick = { viewModel.playTrackPojos(selectedTracks) },
                    onPlayNextClick = { viewModel.playTrackPojosNext(selectedTracks, context) },
                    onUnselectAllClick = { viewModel.unselectAllTracks() },
                )
            )

            LazyColumn(modifier = modifier.padding(horizontal = 10.dp)) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = appCallbacks.onBackClick,
                            content = { Icon(Icons.AutoMirrored.Sharp.ArrowBack, stringResource(R.string.go_back)) },
                            modifier = Modifier.width(40.dp),
                        )
                        if (pojo.album.isOnYoutube) {
                            LargeIconBadge {
                                Icon(painterResource(R.drawable.youtube), null, modifier = Modifier.height(20.dp))
                                Text(text = stringResource(R.string.youtube))
                            }
                        }
                        if (pojo.album.isLocal) {
                            LargeIconBadge {
                                Icon(painterResource(R.drawable.hard_drive), null, modifier = Modifier.height(20.dp))
                                Text(text = stringResource(R.string.local))
                            }
                        }
                    }
                }
                item {
                    var height by remember { mutableStateOf(0.dp) }

                    BoxWithConstraints {
                        height = this.maxWidth * 0.4f

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(modifier = Modifier.weight(0.4f)) {
                                Thumbnail(
                                    image = albumArt,
                                    shape = MaterialTheme.shapes.extraSmall,
                                    placeholder = { Image(Icons.Sharp.Album, null) },
                                )
                            }

                            Column(
                                modifier = Modifier.weight(0.6f).heightIn(min = height),
                                verticalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                Column {
                                    Text(
                                        text = pojo.album.title,
                                        style = if (pojo.album.title.length > 35) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (pojo.album.artist != null) {
                                        Text(
                                            text = pojo.album.artist,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    AlbumBadges(
                                        genres = pojo.genres.map { it.genreName },
                                        styles = pojo.styles.map { it.styleName },
                                    )
                                }

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    AlbumButtons(
                                        isLocal = pojo.album.isLocal,
                                        isInLibrary = pojo.album.isInLibrary,
                                        modifier = Modifier.align(Alignment.Bottom),
                                        isDownloading = downloadProgress != null,
                                        callbacks = AlbumCallbacks.fromAppCallbacks(
                                            album = pojo.album,
                                            appCallbacks = appCallbacks,
                                            onPlayClick = { viewModel.playAlbum() },
                                            onPlayNextClick = { viewModel.playAlbumNext(context) },
                                            onRemoveFromLibraryClick = {
                                                viewModel.removeAlbumFromLibrary(pojo.album)
                                                appCallbacks.onBackClick()
                                            },
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                downloadProgress?.let { progress ->
                    item {
                        val statusText = stringResource(progress.status.stringId)

                        Column {
                            Text(
                                text = "$statusText ${progress.item} …",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                progress = progress.progress.toFloat(),
                            )
                        }
                    }
                }

                items(pojo.trackPojos) { trackPojo ->
                    viewModel.loadTrackMetadata(trackPojo.track)

                    AlbumTrackRow(
                        data = AlbumTrackRowData(
                            title = trackPojo.track.title,
                            isDownloadable = trackPojo.track.isDownloadable,
                            artist = trackPojo.artist,
                            duration = trackPojo.track.metadata?.duration,
                            albumPosition = trackPojo.track.albumPosition,
                            discNumber = trackPojo.track.discNumber,
                            downloadProgress = trackDownloadProgressMap[trackPojo.trackId],
                            showArtist = trackPojo.artist != pojo.album.artist,
                            showDiscNumber = pojo.discCount > 1,
                            isSelected = selectedTracks.contains(trackPojo),
                        ),
                        callbacks = TrackCallbacks.fromAppCallbacks(
                            pojo = trackPojo,
                            appCallbacks = appCallbacks,
                            onTrackClick = {
                                if (selectedTracks.isNotEmpty()) viewModel.toggleSelected(trackPojo)
                                else viewModel.playAlbum(startAt = trackPojo.track)
                            },
                            onPlayNextClick = { viewModel.playAlbumNext(context) },
                            onLongClick = { viewModel.selectTracksFromLastSelected(to = trackPojo) },
                        ),
                    )
                }
            }
        }
    }
}
