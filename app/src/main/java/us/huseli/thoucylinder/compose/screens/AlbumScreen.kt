package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.album.AlbumBadges
import us.huseli.thoucylinder.compose.album.AlbumButtons
import us.huseli.thoucylinder.compose.album.AlbumTrackRow
import us.huseli.thoucylinder.compose.album.AlbumTrackRowData
import us.huseli.thoucylinder.compose.track.SelectedTracksButtons
import us.huseli.thoucylinder.compose.utils.LargeIconBadge
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.abstr.tracks
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
    val albumCombo by viewModel.albumCombo.collectAsStateWithLifecycle(null)
    val selectedTrackCombos by viewModel.selectedTrackCombos.collectAsStateWithLifecycle()
    val trackDownloadTasks by viewModel.trackDownloadTasks.collectAsStateWithLifecycle(emptyList())
    val trackCombos by viewModel.trackCombos.collectAsStateWithLifecycle(emptyList())
    val albumNotFound by viewModel.albumNotFound.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    if (albumNotFound) {
        appCallbacks.onBackClick()
    }

    albumCombo?.let { combo ->
        if (combo.album.isDeleted) {
            appCallbacks.onBackClick()
        }

        Column {
            SelectedTracksButtons(
                trackCount = selectedTrackCombos.size,
                callbacks = TrackSelectionCallbacks(
                    onAddToPlaylistClick = {
                        appCallbacks.onAddToPlaylistClick(Selection(tracks = selectedTrackCombos.tracks()))
                    },
                    onPlayClick = { viewModel.playTrackCombos(selectedTrackCombos) },
                    onEnqueueClick = { viewModel.enqueueTrackCombos(selectedTrackCombos, context) },
                    onUnselectAllClick = { viewModel.unselectAllTrackCombos() },
                )
            )

            LazyColumn {
                // Youtube / Spotify / Local badges:
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = modifier.padding(horizontal = 10.dp).padding(top = 5.dp),
                    ) {
                        IconButton(
                            onClick = appCallbacks.onBackClick,
                            content = { Icon(Icons.Sharp.ArrowBack, stringResource(R.string.go_back)) },
                            modifier = Modifier.width(40.dp),
                        )
                        combo.album.youtubeWebUrl?.also { youtubeUrl ->
                            LargeIconBadge(modifier = Modifier.clickable { uriHandler.openUri(youtubeUrl) }) {
                                Icon(painterResource(R.drawable.youtube), null, modifier = Modifier.height(20.dp))
                                Text(text = stringResource(R.string.youtube))
                            }
                        }
                        combo.spotifyWebUrl?.also { spotifyUrl ->
                            LargeIconBadge(modifier = Modifier.clickable { uriHandler.openUri(spotifyUrl) }) {
                                Icon(painterResource(R.drawable.spotify), null, modifier = Modifier.height(16.dp))
                                Text(text = stringResource(R.string.spotify))
                            }
                        }
                        if (combo.album.isLocal) {
                            LargeIconBadge {
                                Icon(painterResource(R.drawable.hard_drive_filled), null, Modifier.height(20.dp))
                                Text(text = stringResource(R.string.local))
                            }
                        }
                    }
                }

                // Album cover, headlines, buttons, etc:
                item {
                    BoxWithConstraints(modifier = modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.height(140.dp),
                        ) {
                            Thumbnail(
                                image = albumArt,
                                shape = MaterialTheme.shapes.extraSmall,
                                placeholderIcon = Icons.Sharp.Album,
                                modifier = Modifier.fillMaxHeight(),
                            )

                            Column(
                                modifier = Modifier.fillMaxHeight(),
                                verticalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                Column {
                                    Text(
                                        text = combo.album.title,
                                        style = if (combo.album.title.length > 20) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    combo.album.artist?.also { artist ->
                                        Text(
                                            text = artist,
                                            style = if (artist.length > 35) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    AlbumBadges(
                                        genres = combo.genres.map { it.genreName },
                                        styles = combo.styles.map { it.styleName },
                                        year = combo.yearString,
                                        modifier = Modifier
                                            .padding(vertical = 10.dp)
                                            .fillMaxWidth()
                                            .heightIn(max = 37.dp) // max 2 rows
                                            .clipToBounds(),
                                    )
                                }

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    AlbumButtons(
                                        isLocal = combo.album.isLocal,
                                        isInLibrary = combo.album.isInLibrary,
                                        modifier = Modifier.align(Alignment.Bottom),
                                        isDownloading = downloadIsActive,
                                        isPartiallyDownloaded = combo.isPartiallyDownloaded,
                                        callbacks = AlbumCallbacks(
                                            combo = combo,
                                            appCallbacks = appCallbacks,
                                            context = context,
                                            onPlayClick = { viewModel.playTrackCombos(trackCombos) },
                                            onEnqueueClick = { viewModel.enqueueTrackCombos(trackCombos, context) },
                                            onAddToPlaylistClick = {
                                                appCallbacks.onAddToPlaylistClick(Selection(albumWithTracks = combo))
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
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).padding(top = 5.dp),
                            progress = downloadProgress?.toFloat() ?: 0f,
                        )
                    }
                }

                if (combo.isPartiallyDownloaded && !downloadIsActive) {
                    item {
                        Text(
                            text = stringResource(R.string.this_album_is_only_partially_downloaded),
                            style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }

                val rowDataObjects = trackCombos.map { trackCombo ->
                    AlbumTrackRowData(
                        title = trackCombo.track.title,
                        isDownloadable = trackCombo.track.isDownloadable,
                        artist = trackCombo.artist,
                        duration = trackCombo.track.duration,
                        showArtist = trackCombo.artist != combo.album.artist,
                        isSelected = selectedTrackCombos.contains(trackCombo),
                        downloadTask = trackDownloadTasks.find { it.track.trackId == trackCombo.track.trackId },
                        position = trackCombo.track.getPositionString(combo.discCount),
                        isInLibrary = trackCombo.track.isInLibrary,
                    )
                }
                val positionColumnWidth = rowDataObjects.maxOfOrNull { it.position.length * 10 }?.dp

                itemsIndexed(trackCombos) { index, trackCombo ->
                    viewModel.loadTrackMetadata(trackCombo.track)

                    AlbumTrackRow(
                        data = rowDataObjects[index],
                        positionColumnWidth = positionColumnWidth ?: 40.dp,
                        callbacks = TrackCallbacks(
                            combo = trackCombo,
                            appCallbacks = appCallbacks,
                            context = context,
                            onTrackClick = {
                                if (selectedTrackCombos.isNotEmpty()) viewModel.toggleSelected(trackCombo)
                                else viewModel.playTrackCombos(trackCombos, combo.indexOfTrack(trackCombo.track))
                            },
                            onEnqueueClick = { viewModel.enqueueTrackCombo(trackCombo, context) },
                            onLongClick = {
                                viewModel.selectTrackCombosFromLastSelected(to = trackCombo, allCombos = trackCombos)
                            },
                        ),
                    )
                }
            }
        }
    }
}
