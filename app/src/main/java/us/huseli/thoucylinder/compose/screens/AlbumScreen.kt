package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import us.huseli.thoucylinder.compose.track.SelectedTracksButtons
import us.huseli.thoucylinder.compose.utils.LargeIconBadge
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.pluralStringResource
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.AlbumViewModel

@Composable
fun AlbumScreen(
    modifier: Modifier = Modifier,
    viewModel: AlbumViewModel = hiltViewModel(),
    appCallbacks: AppCallbacks,
    albumCallbacks: AlbumCallbacks,
    trackCallbacks: TrackCallbacks,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val albumArt by viewModel.albumArt.collectAsStateWithLifecycle()
    val albumNotFound by viewModel.albumNotFound.collectAsStateWithLifecycle()
    val importProgress by viewModel.importProgress.collectAsStateWithLifecycle()
    val positionColumnWidthDp by viewModel.positionColumnWidthDp.collectAsStateWithLifecycle()
    val selectedTrackIds by viewModel.selectedTrackIds.collectAsStateWithLifecycle()
    val tagNames by viewModel.tagNames.collectAsStateWithLifecycle()
    val trackUiStates by viewModel.trackUiStates.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (albumNotFound) {
        appCallbacks.onBackClick()
    }

    uiState?.also { state ->
        val albumDownloadState by state.downloadState.collectAsStateWithLifecycle()

        if (state.isDeleted) appCallbacks.onBackClick()

        Column {
            SelectedTracksButtons(
                trackCount = selectedTrackIds.size,
                callbacks = remember { viewModel.getTrackSelectionCallbacks(appCallbacks) },
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
                        state.youtubeWebUrl?.also { youtubeUrl ->
                            LargeIconBadge(modifier = Modifier.clickable { uriHandler.openUri(youtubeUrl) }) {
                                Icon(painterResource(R.drawable.youtube), null, modifier = Modifier.height(20.dp))
                                Text(text = stringResource(R.string.youtube))
                            }
                        }
                        state.spotifyWebUrl?.also { spotifyUrl ->
                            LargeIconBadge(modifier = Modifier.clickable { uriHandler.openUri(spotifyUrl) }) {
                                Icon(painterResource(R.drawable.spotify), null, modifier = Modifier.height(16.dp))
                                Text(text = stringResource(R.string.spotify))
                            }
                        }
                        if (state.isLocal) {
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
                            imageBitmap = { albumArt },
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
                                    text = state.title.umlautify(),
                                    style = if (state.title.length > 20) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                state.artistString?.also { artistString ->
                                    Text(
                                        text = artistString.umlautify(),
                                        style = if (artistString.length > 35) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }

                            AlbumBadges(
                                tags = tagNames,
                                year = state.yearString,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 37.dp) // max 2 rows
                                    .clipToBounds(),
                            )

                            Row(modifier = Modifier.fillMaxWidth()) {
                                AlbumButtons(
                                    modifier = Modifier.align(Alignment.Bottom),
                                    albumId = state.albumId,
                                    albumArtists = state.artists,
                                    isLocal = state.isLocal,
                                    isInLibrary = state.isInLibrary,
                                    isDownloading = albumDownloadState?.isActive == true,
                                    isPartiallyDownloaded = state.isPartiallyDownloaded,
                                    youtubeWebUrl = state.youtubeWebUrl,
                                    spotifyWebUrl = state.spotifyWebUrl,
                                    callbacks = remember {
                                        albumCallbacks.copy(
                                            onPlayClick = if (state.isPlayable) {
                                                { viewModel.playAlbum(state.albumId) }
                                            } else null,
                                            onEnqueueClick = if (state.isPlayable) {
                                                { viewModel.enqueueAlbum(state.albumId) }
                                            } else null,
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                albumDownloadState?.takeIf { it.isActive }?.also {
                    item {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).padding(top = 5.dp),
                            progress = { it.progress },
                        )
                    }
                } ?: run {
                    if (state.isPartiallyDownloaded) {
                        item {
                            Text(
                                text = stringResource(R.string.this_album_is_only_partially_downloaded),
                                style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                                modifier = Modifier.padding(10.dp),
                            )
                        }
                    }
                }

                if (state.unplayableTrackCount > 0) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                        ) {
                            Text(
                                text = pluralStringResource(
                                    R.plurals.x_album_tracks_unplayable,
                                    state.unplayableTrackCount,
                                    state.unplayableTrackCount,
                                ),
                                style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedButton(
                                onClick = { viewModel.matchUnplayableTracks(context) },
                                content = { Text(stringResource(R.string.match)) },
                                shape = MaterialTheme.shapes.small,
                                enabled = importProgress.isActive,
                            )
                        }
                    }
                }

                if (importProgress.isActive) {
                    item {
                        ImportProgressSection(
                            progress = importProgress,
                            modifier = Modifier.padding(horizontal = 10.dp),
                        )
                    }
                }

                itemsIndexed(trackUiStates, key = { _, state -> state.trackId }) { index, trackState ->
                    if (!trackState.isPlayable) viewModel.ensureTrackMetadataAsync(trackState.trackId)

                    Spacer(modifier = Modifier.height(5.dp))

                    AlbumTrackRow(
                        state = trackState,
                        position = trackState.positionString,
                        showArtist = trackState.trackArtistString != state.artistString,
                        positionColumnWidth = positionColumnWidthDp.dp,
                        isSelected = selectedTrackIds.contains(trackState.trackId),
                        callbacks = remember {
                            trackCallbacks.copy(
                                onTrackClick = {
                                    if (selectedTrackIds.isNotEmpty()) viewModel.toggleTrackSelected(it)
                                    else if (trackState.isPlayable) viewModel.playAlbum(state.albumId, index)
                                },
                                onLongClick = { viewModel.selectTracksFromLastSelected(to = it) },
                            )
                        },
                    )
                }
            }
        }
    }
}
