package us.huseli.thoucylinder.compose.screens

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
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material.icons.sharp.ArrowBack
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
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.album.AlbumBadges
import us.huseli.thoucylinder.compose.album.AlbumButtons
import us.huseli.thoucylinder.compose.album.AlbumTrackRow
import us.huseli.thoucylinder.compose.album.AlbumTrackRowData
import us.huseli.thoucylinder.compose.track.SelectedTracksButtons
import us.huseli.thoucylinder.compose.utils.LargeIconBadge
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.getDownloadProgress
import us.huseli.thoucylinder.viewmodels.AlbumViewModel

@Composable
fun AlbumScreen(
    modifier: Modifier = Modifier,
    viewModel: AlbumViewModel = hiltViewModel(),
    appCallbacks: AppCallbacks,
) {
    val albumArt by viewModel.albumArt.collectAsStateWithLifecycle(null)
    val (downloadProgress, downloadIsActive) = getDownloadProgress(
        viewModel.albumDownloadTask.collectAsStateWithLifecycle(null)
    )
    val albumPojo by viewModel.albumPojo.collectAsStateWithLifecycle(null)
    val albumWasDeleted by viewModel.albumWasDeleted.collectAsStateWithLifecycle()
    val selectedTrackPojos by viewModel.selectedTrackPojos.collectAsStateWithLifecycle()
    val trackDownloadTasks by viewModel.trackDownloadTasks.collectAsStateWithLifecycle(emptyList())
    val context = LocalContext.current

    if (albumWasDeleted) {
        SnackbarEngine.addInfo(stringResource(R.string.the_album_was_deleted))
        appCallbacks.onBackClick()
    }

    albumPojo?.let { pojo ->
        Column {
            SelectedTracksButtons(
                trackCount = selectedTrackPojos.size,
                callbacks = TrackSelectionCallbacks(
                    onAddToPlaylistClick = { appCallbacks.onAddToPlaylistClick(Selection(trackPojos = selectedTrackPojos)) },
                    onPlayClick = { viewModel.playTrackPojos(selectedTrackPojos) },
                    onEnqueueClick = { viewModel.enqueueTrackPojos(selectedTrackPojos, context) },
                    onUnselectAllClick = { viewModel.unselectAllTrackPojos() },
                )
            )

            LazyColumn(modifier = modifier.padding(horizontal = 10.dp)) {
                // Youtube / Spotify / Local badges:
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = appCallbacks.onBackClick,
                            content = { Icon(Icons.Sharp.ArrowBack, stringResource(R.string.go_back)) },
                            modifier = Modifier.width(40.dp),
                        )
                        if (pojo.isOnYoutube) {
                            LargeIconBadge {
                                Icon(painterResource(R.drawable.youtube), null, modifier = Modifier.height(20.dp))
                                Text(text = stringResource(R.string.youtube))
                            }
                        }
                        if (pojo.isFromSpotify) {
                            LargeIconBadge {
                                Icon(painterResource(R.drawable.spotify), null, modifier = Modifier.height(16.dp))
                                Text(text = stringResource(R.string.spotify))
                            }
                        }
                        if (pojo.isLocal) {
                            LargeIconBadge {
                                Icon(painterResource(R.drawable.hard_drive), null, modifier = Modifier.height(20.dp))
                                Text(text = stringResource(R.string.local))
                            }
                        }
                    }
                }

                // Album cover, headlines, buttons, etc:
                item {
                    var height by remember { mutableStateOf(0.dp) }

                    BoxWithConstraints {
                        height = this.maxWidth * 0.4f

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(modifier = Modifier.weight(0.4f)) {
                                Thumbnail(
                                    image = albumArt,
                                    shape = MaterialTheme.shapes.extraSmall,
                                    placeholderIcon = Icons.Sharp.Album,
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
                                        isDownloading = downloadIsActive,
                                        isPartiallyDownloaded = pojo.isPartiallyDownloaded,
                                        callbacks = AlbumCallbacks.fromAppCallbacks(
                                            album = pojo.album,
                                            appCallbacks = appCallbacks,
                                            onPlayClick = { viewModel.playAlbum() },
                                            onEnqueueClick = { viewModel.enqueueAlbum(context) },
                                            onRemoveFromLibraryClick = {
                                                appCallbacks.onRemoveAlbumFromLibraryClick(pojo.album)
                                                appCallbacks.onBackClick()
                                            },
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                if (downloadIsActive) {
                    item {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            progress = downloadProgress?.toFloat() ?: 0f,
                        )
                    }
                }

                if (pojo.isPartiallyDownloaded && !downloadIsActive) {
                    item {
                        Text(
                            text = stringResource(R.string.this_album_is_only_partially_downloaded),
                            style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                            modifier = Modifier.padding(vertical = 10.dp),
                        )
                    }
                }

                items(pojo.trackPojos) { trackPojo ->
                    val track = trackPojo.track

                    viewModel.loadTrackMetadata(track)

                    AlbumTrackRow(
                        data = AlbumTrackRowData(
                            title = track.title,
                            isDownloadable = track.isDownloadable,
                            artist = trackPojo.artist,
                            duration = track.duration,
                            albumPosition = track.albumPosition,
                            discNumber = track.discNumber,
                            showArtist = trackPojo.artist != pojo.album.artist,
                            showDiscNumber = pojo.discCount > 1,
                            isSelected = selectedTrackPojos.contains(trackPojo),
                            downloadTask = trackDownloadTasks.find { it.track.trackId == track.trackId },
                        ),
                        callbacks = TrackCallbacks.fromAppCallbacks(
                            pojo = trackPojo,
                            appCallbacks = appCallbacks,
                            onTrackClick = {
                                if (selectedTrackPojos.isNotEmpty()) viewModel.toggleSelected(trackPojo)
                                else viewModel.playAlbum(startAt = track)
                            },
                            onEnqueueClick = { viewModel.enqueueAlbum(context) },
                            onLongClick = { viewModel.selectTrackPojosFromLastSelected(to = trackPojo) },
                        ),
                    )
                }
            }
        }
    }
}
