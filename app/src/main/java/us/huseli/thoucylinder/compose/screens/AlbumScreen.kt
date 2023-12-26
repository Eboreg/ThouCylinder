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
    val albumPojo by viewModel.albumPojo.collectAsStateWithLifecycle(null)
    val selectedTrackPojos by viewModel.selectedTrackPojos.collectAsStateWithLifecycle()
    val trackDownloadTasks by viewModel.trackDownloadTasks.collectAsStateWithLifecycle(emptyList())
    val trackPojos by viewModel.trackPojos.collectAsStateWithLifecycle(emptyList())
    val albumNotFound by viewModel.albumNotFound.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    if (albumNotFound) {
        appCallbacks.onBackClick()
    }

    albumPojo?.let { pojo ->
        if (pojo.album.isDeleted) {
            appCallbacks.onBackClick()
        }

        Column {
            SelectedTracksButtons(
                trackCount = selectedTrackPojos.size,
                callbacks = TrackSelectionCallbacks(
                    onAddToPlaylistClick = {
                        appCallbacks.onAddToPlaylistClick(Selection(tracks = selectedTrackPojos.tracks()))
                    },
                    onPlayClick = { viewModel.playTrackPojos(selectedTrackPojos) },
                    onEnqueueClick = { viewModel.enqueueTrackPojos(selectedTrackPojos, context) },
                    onUnselectAllClick = { viewModel.unselectAllTrackPojos() },
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
                        pojo.album.youtubeWebUrl?.also { youtubeUrl ->
                            LargeIconBadge(modifier = Modifier.clickable { uriHandler.openUri(youtubeUrl) }) {
                                Icon(painterResource(R.drawable.youtube), null, modifier = Modifier.height(20.dp))
                                Text(text = stringResource(R.string.youtube))
                            }
                        }
                        pojo.spotifyWebUrl?.also { spotifyUrl ->
                            LargeIconBadge(modifier = Modifier.clickable { uriHandler.openUri(spotifyUrl) }) {
                                Icon(painterResource(R.drawable.spotify), null, modifier = Modifier.height(16.dp))
                                Text(text = stringResource(R.string.spotify))
                            }
                        }
                        if (pojo.album.isLocal) {
                            LargeIconBadge {
                                Icon(painterResource(R.drawable.hard_drive_filled), null, Modifier.height(20.dp))
                                Text(text = stringResource(R.string.local))
                            }
                        }
                    }
                }

                // Album cover, headlines, buttons, etc:
                item {
                    var height by remember { mutableStateOf(0.dp) }

                    BoxWithConstraints(modifier = modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                        height = this.maxWidth * 0.35f

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.height(height),
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
                                        text = pojo.album.title,
                                        style = if (pojo.album.title.length > 35) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    pojo.album.artist?.also { artist ->
                                        Text(
                                            text = artist,
                                            style = if (artist.length > 35) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    AlbumBadges(
                                        genres = pojo.genres.map { it.genreName },
                                        styles = pojo.styles.map { it.styleName },
                                        year = pojo.yearString,
                                        modifier = Modifier
                                            .padding(vertical = 10.dp)
                                            .fillMaxWidth()
                                            .heightIn(max = 37.dp) // max 2 rows
                                            .clipToBounds(),
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
                                            pojo = pojo,
                                            appCallbacks = appCallbacks,
                                            context = context,
                                            onPlayClick = { viewModel.playTrackPojos(trackPojos) },
                                            onEnqueueClick = { viewModel.enqueueTrackPojos(trackPojos, context) },
                                            onRemoveFromLibraryClick = {
                                                appCallbacks.onRemoveAlbumFromLibraryClick(pojo.album)
                                                appCallbacks.onBackClick()
                                            },
                                            onAddToPlaylistClick = {
                                                appCallbacks.onAddToPlaylistClick(Selection(albumWithTracks = pojo))
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

                if (pojo.isPartiallyDownloaded && !downloadIsActive) {
                    item {
                        Text(
                            text = stringResource(R.string.this_album_is_only_partially_downloaded),
                            style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }

                items(trackPojos) { trackPojo ->
                    viewModel.loadTrackMetadata(trackPojo.track)

                    AlbumTrackRow(
                        data = AlbumTrackRowData(
                            title = trackPojo.track.title,
                            isDownloadable = trackPojo.track.isDownloadable,
                            artist = trackPojo.artist,
                            duration = trackPojo.track.duration,
                            albumPosition = trackPojo.track.albumPosition,
                            discNumber = trackPojo.track.discNumber,
                            showArtist = trackPojo.artist != pojo.album.artist,
                            showDiscNumber = pojo.discCount > 1,
                            isSelected = selectedTrackPojos.contains(trackPojo),
                            downloadTask = trackDownloadTasks.find { it.track.trackId == trackPojo.track.trackId },
                        ),
                        callbacks = TrackCallbacks.fromAppCallbacks(
                            pojo = trackPojo,
                            appCallbacks = appCallbacks,
                            context = context,
                            onTrackClick = {
                                if (selectedTrackPojos.isNotEmpty()) viewModel.toggleSelected(trackPojo)
                                else viewModel.playTrackPojos(trackPojos, pojo.indexOfTrack(trackPojo.track))
                            },
                            onEnqueueClick = { viewModel.enqueueTrackPojo(trackPojo, context) },
                            onLongClick = {
                                viewModel.selectTrackPojosFromLastSelected(to = trackPojo, allPojos = trackPojos)
                            },
                        ),
                    )
                }
            }
        }
    }
}
