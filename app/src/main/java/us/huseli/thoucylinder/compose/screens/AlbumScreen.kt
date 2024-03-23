package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.ImportProgressSection
import us.huseli.thoucylinder.compose.album.AlbumBadges
import us.huseli.thoucylinder.compose.album.AlbumButtons
import us.huseli.thoucylinder.compose.album.AlbumTrackRow
import us.huseli.thoucylinder.compose.album.AlbumTrackRowData
import us.huseli.thoucylinder.compose.track.SelectedTracksButtons
import us.huseli.thoucylinder.compose.utils.LargeIconBadge
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.getDownloadProgress
import us.huseli.thoucylinder.pluralStringResource
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.AlbumViewModel

@Composable
fun AlbumScreen(
    modifier: Modifier = Modifier,
    viewModel: AlbumViewModel = hiltViewModel(),
    appCallbacks: AppCallbacks,
    onAlbumComboFetched: (AbstractAlbumCombo) -> Unit = {},
) {
    val albumArt by viewModel.albumArt.collectAsStateWithLifecycle(null)
    val (downloadProgress, downloadIsActive) = getDownloadProgress(
        viewModel.albumDownloadTask.collectAsStateWithLifecycle(null)
    )
    val albumCombo by viewModel.albumCombo.collectAsStateWithLifecycle(null)
    val selectedTrackIds by viewModel.selectedTrackIds.collectAsStateWithLifecycle()
    val trackDownloadTasks by viewModel.trackDownloadTasks.collectAsStateWithLifecycle(emptyList())
    val albumNotFound by viewModel.albumNotFound.collectAsStateWithLifecycle()
    val importProgress by viewModel.importProgress.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var isPlayable by rememberSaveable { mutableStateOf(true) }

    if (albumNotFound) {
        appCallbacks.onBackClick()
    }

    LaunchedEffect(albumCombo) {
        albumCombo?.also(onAlbumComboFetched)
    }

    albumCombo?.let { combo ->
        val artistString = combo.artists.joined()

        if (combo.album.isDeleted) appCallbacks.onBackClick()

        LaunchedEffect(combo) {
            isPlayable = combo.album.isLocal || combo.album.youtubePlaylist != null
        }

        Column {
            SelectedTracksButtons(
                trackCount = selectedTrackIds.size,
                callbacks = viewModel.getTrackSelectionCallbacks(appCallbacks, context),
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
                            content = { Icon(Icons.AutoMirrored.Sharp.ArrowBack, stringResource(R.string.go_back)) },
                            modifier = Modifier.width(40.dp),
                        )
                        combo.album.youtubeWebUrl?.also { youtubeUrl ->
                            LargeIconBadge(modifier = Modifier.clickable { uriHandler.openUri(youtubeUrl) }) {
                                Icon(painterResource(R.drawable.youtube), null, modifier = Modifier.height(20.dp))
                                Text(text = stringResource(R.string.youtube))
                            }
                        }
                        combo.album.spotifyWebUrl?.also { spotifyUrl ->
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.height(150.dp).padding(horizontal = 10.dp, vertical = 5.dp),
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
                                    text = combo.album.title.umlautify(),
                                    style = if (combo.album.title.length > 20) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (artistString != null) Text(
                                    text = artistString.umlautify(),
                                    style = if (artistString.length > 35) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            AlbumBadges(
                                tags = combo.tags.map { it.name },
                                year = combo.yearString,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 37.dp) // max 2 rows
                                    .clipToBounds(),
                            )

                            Row(modifier = Modifier.fillMaxWidth()) {
                                AlbumButtons(
                                    albumArtists = combo.artists,
                                    isLocal = combo.album.isLocal,
                                    isInLibrary = combo.album.isInLibrary,
                                    modifier = Modifier.align(Alignment.Bottom),
                                    isDownloading = downloadIsActive,
                                    isPartiallyDownloaded = combo.isPartiallyDownloaded,
                                    callbacks = AlbumCallbacks(
                                        combo = combo,
                                        appCallbacks = appCallbacks,
                                        context = context,
                                        onPlayClick = if (isPlayable) {
                                            { viewModel.playTrackCombos(combo.trackCombos) }
                                        } else null,
                                        onEnqueueClick = if (isPlayable) {
                                            { viewModel.enqueueTrackCombos(combo.trackCombos, context) }
                                        } else null,
                                        onAddToPlaylistClick = {
                                            appCallbacks.onAddToPlaylistClick(Selection(albumWithTracks = combo))
                                        },
                                    )
                                )
                            }
                        }
                    }
                }

                if (downloadIsActive) {
                    item {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).padding(top = 5.dp),
                            progress = { downloadProgress?.toFloat() ?: 0f },
                        )
                    }
                } else if (combo.isPartiallyDownloaded) {
                    item {
                        Text(
                            text = stringResource(R.string.this_album_is_only_partially_downloaded),
                            style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }

                if (!combo.album.isLocal && combo.album.youtubePlaylist == null) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                        ) {
                            Text(
                                text = pluralStringResource(
                                    R.plurals.x_album_tracks_unplayable,
                                    combo.unplayableTrackCount,
                                    combo.unplayableTrackCount,
                                ),
                                style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedButton(
                                onClick = { viewModel.matchUnplayableTracks(context) },
                                content = { Text(stringResource(R.string.match)) },
                                shape = MaterialTheme.shapes.small,
                                enabled = importProgress == null,
                            )
                        }
                    }
                }

                importProgress?.also { progress ->
                    item {
                        ImportProgressSection(progress = progress, modifier = Modifier.padding(horizontal = 10.dp))
                    }
                }

                val rowDataObjects = combo.trackCombos.map { trackCombo ->
                    AlbumTrackRowData(
                        title = trackCombo.track.title,
                        isDownloadable = trackCombo.track.isDownloadable,
                        duration = trackCombo.track.duration,
                        showArtist = trackCombo.artists.joined() != artistString,
                        isSelected = selectedTrackIds.contains(trackCombo.track.trackId),
                        downloadTask = trackDownloadTasks.find { it.trackCombo.track.trackId == trackCombo.track.trackId },
                        position = trackCombo.track.getPositionString(combo.discCount),
                        isInLibrary = trackCombo.track.isInLibrary,
                        artists = trackCombo.artists,
                        isPlayable = trackCombo.track.isPlayable,
                    )
                }
                val positionColumnWidth = rowDataObjects.maxOfOrNull { it.position.length * 10 }?.dp

                itemsIndexed(combo.trackCombos) { index, trackCombo ->
                    viewModel.ensureTrackMetadataAsync(trackCombo.track)

                    AlbumTrackRow(
                        data = rowDataObjects[index],
                        positionColumnWidth = positionColumnWidth ?: 40.dp,
                        callbacks = TrackCallbacks(
                            combo = trackCombo,
                            appCallbacks = appCallbacks,
                            context = context,
                            onTrackClick = {
                                if (selectedTrackIds.isNotEmpty()) viewModel.toggleTrackSelected(trackCombo.track.trackId)
                                else if (trackCombo.track.isPlayable)
                                    viewModel.playTrackCombos(combo.trackCombos, combo.indexOfTrack(trackCombo.track))
                            },
                            onEnqueueClick = if (trackCombo.track.isPlayable) {
                                { viewModel.enqueueTrackCombo(trackCombo, context) }
                            } else null,
                            onLongClick = {
                                viewModel.selectTracksFromLastSelected(
                                    to = trackCombo.track.trackId,
                                    allTrackIds = combo.trackCombos.map { it.track.trackId },
                                )
                            },
                        ),
                    )
                }
            }
        }
    }
}
