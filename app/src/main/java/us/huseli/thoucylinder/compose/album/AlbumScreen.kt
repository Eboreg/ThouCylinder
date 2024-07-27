package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.compose.ProgressSection
import us.huseli.thoucylinder.compose.artist.OtherArtistAlbumsHeader
import us.huseli.thoucylinder.compose.artist.OtherArtistAlbumsList
import us.huseli.thoucylinder.compose.track.SelectedTracksButtons
import us.huseli.thoucylinder.compose.utils.BasicHeader
import us.huseli.thoucylinder.compose.utils.DownloadStateProgressIndicator
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.album.AlbumUiState
import us.huseli.thoucylinder.dataclasses.album.LocalAlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.LocalAppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.LocalAppDialogCallbacks
import us.huseli.thoucylinder.dataclasses.musicbrainz.joined
import us.huseli.thoucylinder.getClickableArtist
import us.huseli.thoucylinder.getClickableArtists
import us.huseli.thoucylinder.pluralStringResource
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.AlbumViewModel

@Composable
fun AlbumScreen(viewModel: AlbumViewModel = hiltViewModel()) {
    val appCallbacks = LocalAppCallbacks.current
    val albumNotFound by viewModel.albumNotFound.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (albumNotFound) appCallbacks.onBackClick()
    uiState?.also { state -> AlbumScreen(state = state, viewModel = viewModel) }
}

@Composable
fun AlbumScreen(state: AlbumUiState, viewModel: AlbumViewModel) {
    val appCallbacks = LocalAppCallbacks.current
    val dialogCallbacks = LocalAppDialogCallbacks.current
    val albumCallbacks = LocalAlbumCallbacks.current
    val context = LocalContext.current

    val importProgress by viewModel.importProgress.collectAsStateWithLifecycle()
    val positionColumnWidthDp by viewModel.positionColumnWidthDp.collectAsStateWithLifecycle()
    val selectedTrackCount by viewModel.selectedTrackCount.collectAsStateWithLifecycle()
    val tagNames by viewModel.tagNames.collectAsStateWithLifecycle()
    val trackUiStates by viewModel.trackUiStates.collectAsStateWithLifecycle()
    val otherArtistAlbums by viewModel.otherArtistAlbums.collectAsStateWithLifecycle()
    val albumDownloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val musicBrainzReleases by viewModel.musicBrainzReleases.collectAsStateWithLifecycle()

    Column {
        BasicHeader(title = state.title.umlautify()) {
            AlbumBottomSheetWithButton(uiState = state)
        }

        SelectedTracksButtons(
            trackCount = { selectedTrackCount },
            callbacks = remember { viewModel.getTrackSelectionCallbacks(dialogCallbacks) },
        )

        LazyColumn(contentPadding = PaddingValues(10.dp)) {
            // Youtube / Spotify / Local badges:
            item { AlbumSourceBadges(state = state) }

            // Album cover, artists, tags, buttons, etc:
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.height(150.dp).padding(bottom = 10.dp),
                ) {
                    Thumbnail(
                        model = state,
                        placeholderIcon = Icons.Sharp.Album,
                        modifier = Modifier.fillMaxHeight(),
                    )

                    Column(
                        modifier = Modifier.fillMaxHeight().padding(vertical = 5.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val artists = getClickableArtists(
                            artists = state.artists,
                            onArtistClick = appCallbacks.onGotoArtistClick,
                        )

                        if (artists.isNotBlank()) {
                            Text(
                                text = artists,
                                style = if (artists.length > 35) FistopyTheme.typography.titleMedium else FistopyTheme.typography.titleLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        AlbumTagBadges(
                            tags = tagNames,
                            year = state.yearString,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 39.dp) // max 2 rows
                                .clipToBounds(),
                        )

                        AlbumButtons(
                            uiState = state,
                            isDownloading = albumDownloadState?.isActive == true,
                        )
                    }
                }
            }

            // Download progress:
            albumDownloadState?.takeIf { it.isActive }?.also {
                item {
                    DownloadStateProgressIndicator(
                        downloadState = it,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                }
            } ?: run {
                if (state.isPartiallyDownloaded) {
                    item {
                        Text(
                            text = stringResource(R.string.this_album_is_only_partially_downloaded),
                            style = FistopyTheme.bodyStyles.secondarySmall,
                            modifier = Modifier.padding(bottom = 5.dp),
                        )
                    }
                }
            }

            // Unplayable tracks info & CTA:
            if (state.unplayableTrackCount > 0) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp),
                    ) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.x_album_tracks_unplayable,
                                state.unplayableTrackCount,
                                state.unplayableTrackCount,
                            ),
                            style = FistopyTheme.bodyStyles.secondarySmall,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(
                            onClick = { viewModel.matchUnplayableTracks() },
                            content = { Text(stringResource(R.string.match)) },
                            shape = MaterialTheme.shapes.small,
                            enabled = !importProgress.isActive,
                        )
                    }
                }
            }

            item { ProgressSection(progress = importProgress) }

            items(musicBrainzReleases) { release ->
                val about = listOfNotNull(
                    release.date,
                    release.getCountryName(context),
                    "${release.trackCount} tracks",
                    release.labelInfo?.joinToString("/") { it.label.name },
                ).joinToString(" / ")

                CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodySmall) {
                    Column {
                        Text(text = "${release.artistCredit.joined()} - ${release.title}")
                        Text(text = about)
                    }
                }
            }

            // Track list:
            itemsIndexed(trackUiStates, key = { _, state -> state.id }) { index, trackState ->
                val downloadState =
                    viewModel.getTrackDownloadUiStateFlow(trackState.id).collectAsStateWithLifecycle()

                if (!trackState.isPlayable) viewModel.ensureTrackMetadataAsync(trackState.trackId)

                AlbumTrackRow(
                    state = trackState,
                    downloadState = downloadState,
                    position = trackState.positionString,
                    onClick = { viewModel.onTrackClick(trackState) },
                    onLongClick = { viewModel.onTrackLongClick(trackState.id) },
                    positionColumnWidth = positionColumnWidthDp.dp,
                    showArtist = trackState.artistString != state.artistString,
                    containerColor = if (index % 2 == 0) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent,
                )
            }

            for (data in otherArtistAlbums) {
                item { Spacer(modifier = Modifier.height(40.dp)) }

                OtherArtistAlbumsList(
                    isExpanded = data.isExpanded,
                    albums = data.albums,
                    preview = data.preview,
                    albumTypes = data.albumTypes,
                    onClick = { viewModel.onOtherArtistAlbumClick(it, albumCallbacks.onGotoAlbumClick) },
                    onAlbumTypeClick = { viewModel.toggleOtherArtistAlbumsAlbumType(data, it) },
                    header = {
                        OtherArtistAlbumsHeader(
                            expand = data.isExpanded,
                            text = {
                                Text(
                                    text = getClickableArtist(
                                        artist = data.artist,
                                        onArtistClick = appCallbacks.onGotoArtistClick,
                                        prefix = context.getString(R.string.other_albums_by) + " ",
                                        spanStyle = SpanStyle(fontWeight = FontWeight.Bold),
                                    ),
                                    style = FistopyTheme.typography.titleMedium,
                                    maxLines = 2,
                                )
                            },
                            onExpandToggleClick = { viewModel.toggleOtherArtistAlbumsExpanded(data) },
                        )
                    },
                )
            }
        }
    }
}
