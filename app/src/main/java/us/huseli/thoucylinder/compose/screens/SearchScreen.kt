package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.InputChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.SearchType
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.compose.album.AlbumGrid
import us.huseli.thoucylinder.compose.album.AlbumList
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListDisplayTypeButton
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.compose.ListTypeChips
import us.huseli.thoucylinder.compose.SearchForm
import us.huseli.thoucylinder.compose.track.TrackGrid
import us.huseli.thoucylinder.compose.track.TrackList
import us.huseli.thoucylinder.compose.utils.ObnoxiousProgressIndicator
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.pojos.AlbumPojo
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import us.huseli.thoucylinder.viewmodels.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
    appCallbacks: AppCallbacks,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isSearchingLocalAlbums by viewModel.isSearchingLocalAlbums.collectAsStateWithLifecycle()
    val isSearchingLocalTracks by viewModel.isSearchingLocalTracks.collectAsStateWithLifecycle()
    val isSearchingYoutubeAlbums by viewModel.isSearchingYoutubeAlbums.collectAsStateWithLifecycle()
    val isSearchingYoutubeTracks by viewModel.isSearchingYoutubeTracks.collectAsStateWithLifecycle()
    val localAlbums by viewModel.localAlbumPojos.collectAsStateWithLifecycle(emptyList())
    val localTracks = viewModel.localTracks.collectAsLazyPagingItems()
    val selectedLocalAlbumPojos by viewModel.selectedLocalAlbumPojos.collectAsStateWithLifecycle(emptyList())
    val selectedLocalTracks by viewModel.selectedTracks.collectAsStateWithLifecycle(emptyList())
    val selectedYoutubeAlbumPojos by viewModel.selectedYoutubeAlbumPojos.collectAsStateWithLifecycle(emptyList())
    val selectedYoutubeTracks by viewModel.selectedYoutubeTracks.collectAsStateWithLifecycle(emptyList())
    val youtubeAlbums by viewModel.youtubeAlbums.collectAsStateWithLifecycle(emptyList())
    val youtubeTracks = viewModel.youtubeTracksPaging.collectAsLazyPagingItems()

    var displaySearchType by rememberSaveable { mutableStateOf(SearchType.YOUTUBE) }
    var displayType by rememberSaveable { mutableStateOf(DisplayType.LIST) }
    var listType by rememberSaveable { mutableStateOf(ListType.ALBUMS) }

    val withTempTracks: (List<TrackPojo>, (List<TrackPojo>) -> Unit) -> (() -> Unit) = { pojos, callback ->
        { scope.launch { callback(viewModel.ensureTrackMetadata(pojos)) } }
    }

    val withTempTrack: (TrackPojo, (TrackPojo) -> Unit) -> (() -> Unit) = { track, callback ->
        withTempTracks(listOf(track)) { callback(it.first()) }
    }

    val isSearching = when (listType) {
        ListType.ALBUMS -> when (displaySearchType) {
            SearchType.LOCAL -> isSearchingLocalAlbums
            SearchType.YOUTUBE -> isSearchingYoutubeAlbums
        }
        ListType.TRACKS -> when (displaySearchType) {
            SearchType.LOCAL -> isSearchingLocalTracks
            SearchType.YOUTUBE -> isSearchingYoutubeTracks
        }
        ListType.ARTISTS -> false
        ListType.PLAYLISTS -> false
    }

    Column(modifier = modifier) {
        SearchForm(
            modifier = Modifier.padding(horizontal = 10.dp),
            isSearching = isSearching,
            onSearch = { query -> viewModel.search(query) }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp).weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                InputChip(
                    selected = displaySearchType == SearchType.LOCAL,
                    onClick = { displaySearchType = SearchType.LOCAL },
                    label = { Text(text = stringResource(R.string.local)) },
                )
                InputChip(
                    selected = displaySearchType == SearchType.YOUTUBE,
                    onClick = { displaySearchType = SearchType.YOUTUBE },
                    label = { Text(text = stringResource(R.string.youtube)) },
                )
                ListTypeChips(
                    current = listType,
                    onChange = { listType = it },
                    exclude = listOf(ListType.ARTISTS, ListType.PLAYLISTS),
                )
            }
            ListDisplayTypeButton(current = displayType, onChange = { displayType = it })
        }

        if (isSearching) {
            ObnoxiousProgressIndicator(modifier = Modifier.padding(10.dp), tonalElevation = 5.dp)
        }

        when (displaySearchType) {
            SearchType.LOCAL -> {
                when (listType) {
                    ListType.ALBUMS -> AlbumSearchResults(
                        albumPojos = localAlbums,
                        displayType = displayType,
                        selectedAlbumPojos = selectedLocalAlbumPojos,
                        isSearching = isSearchingLocalAlbums,
                        albumCallbacks = { pojo: AbstractAlbumPojo ->
                            AlbumCallbacks.fromAppCallbacks(
                                album = pojo.album,
                                appCallbacks = appCallbacks,
                                onPlayClick = { viewModel.playAlbum(pojo.album) },
                                onEnqueueClick = { viewModel.enqueueAlbum(pojo.album, context) },
                                onAlbumLongClick = { viewModel.selectLocalAlbumsFromLastSelected(pojo as AlbumPojo) },
                                onAlbumClick = {
                                    if (selectedLocalAlbumPojos.isNotEmpty())
                                        viewModel.toggleSelectedLocal(pojo as AlbumPojo)
                                    else appCallbacks.onAlbumClick(pojo.album.albumId)
                                },
                            )
                        },
                        albumSelectionCallbacks = AlbumSelectionCallbacks(
                            onAddToPlaylistClick = {
                                appCallbacks.onAddToPlaylistClick(
                                    Selection(albums = selectedLocalAlbumPojos.map { it.album })
                                )
                            },
                            onPlayClick = { viewModel.playAlbumPojos(selectedLocalAlbumPojos) },
                            onEnqueueClick = { viewModel.enqueueAlbumPojos(selectedLocalAlbumPojos, context) },
                            onUnselectAllClick = { viewModel.unselectAllLocalAlbums() },
                            onSelectAllClick = { viewModel.selectAllLocalAlbums() },
                        ),
                    )
                    ListType.TRACKS -> TrackSearchResults(
                        tracks = localTracks,
                        selectedTracks = selectedLocalTracks,
                        viewModel = viewModel,
                        displayType = displayType,
                        isSearching = isSearchingLocalTracks,
                        trackCallbacks = { pojo: TrackPojo ->
                            TrackCallbacks.fromAppCallbacks(
                                pojo = pojo,
                                appCallbacks = appCallbacks,
                                onEnqueueClick = { viewModel.enqueueTrackPojo(pojo, context) },
                                onTrackClick = {
                                    if (selectedLocalAlbumPojos.isNotEmpty()) viewModel.toggleSelected(pojo)
                                    else viewModel.playTrackPojo(pojo)
                                },
                                onLongClick = { viewModel.selectTracksFromLastSelected(to = pojo) },
                            )
                        },
                        trackSelectionCallbacks = TrackSelectionCallbacks(
                            onAddToPlaylistClick = {
                                appCallbacks.onAddToPlaylistClick(Selection(trackPojos = selectedLocalTracks))
                            },
                            onPlayClick = { viewModel.playTrackPojos(selectedLocalTracks) },
                            onEnqueueClick = { viewModel.enqueueTrackPojos(selectedLocalTracks, context) },
                            onUnselectAllClick = { viewModel.unselectAllTracks() },
                        ),
                    )
                    ListType.ARTISTS -> {}
                    ListType.PLAYLISTS -> {}
                }
            }
            SearchType.YOUTUBE -> {
                when (listType) {
                    ListType.ALBUMS -> AlbumSearchResults(
                        albumPojos = youtubeAlbums,
                        displayType = displayType,
                        selectedAlbumPojos = selectedYoutubeAlbumPojos,
                        isSearching = isSearchingYoutubeAlbums,
                        albumCallbacks = { pojo: AbstractAlbumPojo ->
                            AlbumCallbacks.fromAppCallbacks(
                                album = pojo.album,
                                appCallbacks = appCallbacks,
                                onPlayClick = { viewModel.playTrackPojos((pojo as AlbumWithTracksPojo).trackPojos) },
                                onEnqueueClick = {
                                    viewModel.enqueueTrackPojos((pojo as AlbumWithTracksPojo).trackPojos, context)
                                },
                                onAddToPlaylistClick = {
                                    appCallbacks.onAddToPlaylistClick(
                                        Selection(tracks = (pojo as AlbumWithTracksPojo).tracks)
                                    )
                                },
                                onAlbumLongClick = {
                                    viewModel.selectYoutubeAlbumsFromLastSelected(pojo as AlbumWithTracksPojo)
                                },
                                onAlbumClick = {
                                    if (selectedYoutubeAlbumPojos.isNotEmpty())
                                        viewModel.toggleSelectedYoutube(pojo as AlbumWithTracksPojo)
                                    else appCallbacks.onAlbumClick(pojo.album.albumId)
                                },
                            )
                        },
                        albumSelectionCallbacks = AlbumSelectionCallbacks(
                            onPlayClick = {
                                viewModel.playTrackPojos(selectedYoutubeAlbumPojos.flatMap { it.trackPojos })
                            },
                            onEnqueueClick = {
                                viewModel.enqueueTrackPojos(
                                    selectedYoutubeAlbumPojos.flatMap { it.trackPojos },
                                    context,
                                )
                            },
                            onUnselectAllClick = { viewModel.unselectAllYoutubeAlbums() },
                            onAddToPlaylistClick = {
                                appCallbacks.onAddToPlaylistClick(
                                    Selection(tracks = selectedYoutubeAlbumPojos.flatMap { it.tracks })
                                )
                            },
                            onSelectAllClick = { viewModel.selectAllYoutubeAlbums() },
                        ),
                    )
                    ListType.TRACKS -> TrackSearchResults(
                        tracks = youtubeTracks,
                        selectedTracks = selectedYoutubeTracks,
                        viewModel = viewModel,
                        displayType = displayType,
                        isSearching = isSearchingYoutubeTracks,
                        trackCallbacks = { pojo: TrackPojo ->
                            TrackCallbacks.fromAppCallbacks(
                                pojo = pojo,
                                appCallbacks = appCallbacks,
                                onEnqueueClick = withTempTrack(pojo) { viewModel.enqueueTrackPojo(it, context) },
                                onAddToPlaylistClick = withTempTrack(pojo) {
                                    appCallbacks.onAddToPlaylistClick(Selection(trackPojo = it))
                                },
                                onLongClick = { viewModel.toggleSelectedYoutube(pojo) },
                                onTrackClick = withTempTrack(pojo) {
                                    if (selectedYoutubeAlbumPojos.isNotEmpty()) viewModel.toggleSelectedYoutube(it)
                                    else viewModel.playTrackPojo(it)
                                },
                            )
                        },
                        trackSelectionCallbacks = TrackSelectionCallbacks(
                            onAddToPlaylistClick = withTempTracks(selectedYoutubeTracks) {
                                appCallbacks.onAddToPlaylistClick(Selection(trackPojos = it))
                            },
                            onPlayClick = withTempTracks(selectedYoutubeTracks) { viewModel.playTrackPojos(it) },
                            onEnqueueClick = withTempTracks(selectedYoutubeTracks) {
                                viewModel.enqueueTrackPojos(it, context)
                            },
                            onUnselectAllClick = { viewModel.unselectAllYoutubeTracks() },
                        ),
                    )
                    ListType.ARTISTS -> {}
                    ListType.PLAYLISTS -> {}
                }
            }
        }
    }
}


@Composable
fun AlbumSearchResults(
    albumCallbacks: (AbstractAlbumPojo) -> AlbumCallbacks,
    albumSelectionCallbacks: AlbumSelectionCallbacks,
    albumPojos: List<AbstractAlbumPojo>,
    displayType: DisplayType,
    isSearching: Boolean,
    selectedAlbumPojos: List<AbstractAlbumPojo>,
) {
    when (displayType) {
        DisplayType.LIST -> {
            AlbumList(
                pojos = albumPojos,
                albumCallbacks = albumCallbacks,
                albumSelectionCallbacks = albumSelectionCallbacks,
                selectedAlbums = selectedAlbumPojos.map { it.album },
                onEmpty = {
                    if (!isSearching) Text(stringResource(R.string.no_albums_found), modifier = Modifier.padding(10.dp))
                },
            )
        }
        DisplayType.GRID -> {
            AlbumGrid(
                pojos = albumPojos,
                albumCallbacks = albumCallbacks,
                selectedAlbums = selectedAlbumPojos.map { it.album },
                albumSelectionCallbacks = albumSelectionCallbacks,
                onEmpty = {
                    if (!isSearching) Text(stringResource(R.string.no_albums_found), modifier = Modifier.padding(10.dp))
                },
            )
        }
    }
}


@Composable
fun TrackSearchResults(
    displayType: DisplayType,
    isSearching: Boolean,
    selectedTracks: List<TrackPojo>,
    trackCallbacks: (TrackPojo) -> TrackCallbacks,
    trackSelectionCallbacks: TrackSelectionCallbacks,
    tracks: LazyPagingItems<TrackPojo>,
    viewModel: SearchViewModel,
) {
    when (displayType) {
        DisplayType.LIST -> {
            TrackList(
                trackPojos = tracks,
                selectedTracks = selectedTracks,
                viewModel = viewModel,
                trackCallbacks = trackCallbacks,
                trackSelectionCallbacks = trackSelectionCallbacks,
                onEmpty = {
                    if (!isSearching)
                        Text(stringResource(R.string.no_tracks_found), modifier = Modifier.padding(10.dp))
                },
            )
        }
        DisplayType.GRID -> {
            TrackGrid(
                trackPojos = tracks,
                viewModel = viewModel,
                trackCallbacks = trackCallbacks,
                trackSelectionCallbacks = trackSelectionCallbacks,
                selectedTracks = selectedTracks,
                onEmpty = {
                    if (!isSearching)
                        Text(stringResource(R.string.no_tracks_found), modifier = Modifier.padding(10.dp))
                },
            )
        }
    }
}
