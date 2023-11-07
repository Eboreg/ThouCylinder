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
import us.huseli.thoucylinder.compose.AlbumGrid
import us.huseli.thoucylinder.compose.AlbumList
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListDisplayTypeButton
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.compose.ListTypeChips
import us.huseli.thoucylinder.compose.SearchForm
import us.huseli.thoucylinder.compose.TrackGrid
import us.huseli.thoucylinder.compose.TrackList
import us.huseli.thoucylinder.compose.utils.ObnoxiousProgressIndicator
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.entities.Album
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
    val selectedLocalAlbums by viewModel.selectedAlbums.collectAsStateWithLifecycle(emptyList())
    val selectedLocalTracks by viewModel.selectedTracks.collectAsStateWithLifecycle(emptyList())
    val selectedYoutubeAlbums by viewModel.selectedYoutubeAlbums.collectAsStateWithLifecycle(emptyList())
    val selectedYoutubeTracks by viewModel.selectedYoutubeTracks.collectAsStateWithLifecycle(emptyList())
    val youtubeAlbums by viewModel.youtubeAlbums.collectAsStateWithLifecycle(emptyList())
    val youtubeTracks = viewModel.youtubeTracksPaging.collectAsLazyPagingItems()

    var displaySearchType by rememberSaveable { mutableStateOf(SearchType.YOUTUBE) }
    var displayType by rememberSaveable { mutableStateOf(DisplayType.LIST) }
    var listType by rememberSaveable { mutableStateOf(ListType.ALBUMS) }

    val withTempAlbums: (List<Album>, (List<AlbumWithTracksPojo>) -> Unit) -> Unit = { albums, callback ->
        scope.launch {
            val pojos = viewModel.populateTempAlbums(albums)
            callback(pojos)
        }
    }

    val withTempAlbum: (Album, (AlbumWithTracksPojo) -> Unit) -> Unit = { album, callback ->
        withTempAlbums(listOf(album)) { callback(it.first()) }
    }

    val withTempTracks: (List<TrackPojo>, (List<TrackPojo>) -> Unit) -> (() -> Unit) = { pojos, callback ->
        { scope.launch { callback(viewModel.ensureVideoMetadata(pojos)) } }
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
                        albums = localAlbums,
                        displayType = displayType,
                        selectedAlbums = selectedLocalAlbums,
                        isSearching = isSearchingLocalAlbums,
                        albumCallbacks = { album: Album ->
                            AlbumCallbacks.fromAppCallbacks(
                                album = album,
                                appCallbacks = appCallbacks,
                                onPlayClick = { viewModel.playAlbum(album) },
                                onPlayNextClick = { viewModel.playAlbumNext(album, context) },
                                onRemoveFromLibraryClick = { viewModel.removeAlbumFromLibrary(album) },
                                onAlbumLongClick = { viewModel.selectLocalAlbumsFromLastSelected(album) },
                                onAlbumClick = {
                                    if (selectedLocalAlbums.isNotEmpty()) viewModel.toggleSelected(album)
                                    else appCallbacks.onAlbumClick(album.albumId)
                                },
                            )
                        },
                        albumSelectionCallbacks = AlbumSelectionCallbacks(
                            albums = selectedLocalAlbums,
                            appCallbacks = appCallbacks,
                            onPlayClick = { viewModel.playAlbums(selectedLocalAlbums) },
                            onPlayNextClick = { viewModel.playAlbumsNext(selectedLocalAlbums, context) },
                            onUnselectAllClick = { viewModel.unselectAllAlbums() },
                            onSelectAllClick = { viewModel.selectAlbums(localAlbums.map { it.album }) },
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
                                onPlayNextClick = { viewModel.playTrackPojoNext(pojo, context) },
                                onTrackClick = {
                                    if (selectedLocalAlbums.isNotEmpty()) viewModel.toggleSelected(pojo)
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
                            onPlayNextClick = { viewModel.playTrackPojosNext(selectedLocalTracks, context) },
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
                        albums = youtubeAlbums,
                        displayType = displayType,
                        selectedAlbums = selectedYoutubeAlbums,
                        isSearching = isSearchingYoutubeAlbums,
                        albumCallbacks = { album: Album ->
                            AlbumCallbacks.fromAppCallbacks(
                                album = album,
                                appCallbacks = appCallbacks,
                                onPlayClick = {
                                    withTempAlbum(album) { pojo -> viewModel.playTrackPojos(pojo.trackPojos) }
                                },
                                onPlayNextClick = {
                                    withTempAlbum(album) { pojo ->
                                        viewModel.playTrackPojosNext(pojo.trackPojos, context)
                                    }
                                },
                                onAddToPlaylistClick = {
                                    withTempAlbum(album) { pojo ->
                                        appCallbacks.onAddToPlaylistClick(Selection(tracks = pojo.tracks))
                                    }
                                },
                                onRemoveFromLibraryClick = { viewModel.removeAlbumFromLibrary(album) },
                                onAlbumLongClick = {
                                    withTempAlbum(album) { viewModel.selectYoutubeAlbumsFromLastSelected(album) }
                                },
                                onAlbumClick = {
                                    withTempAlbum(album) {
                                        if (selectedYoutubeAlbums.isNotEmpty()) viewModel.toggleSelectedYoutube(album)
                                        else appCallbacks.onAlbumClick(album.albumId)
                                    }
                                },
                            )
                        },
                        albumSelectionCallbacks = AlbumSelectionCallbacks(
                            onPlayClick = {
                                withTempAlbums(selectedYoutubeAlbums) { pojos ->
                                    viewModel.playTrackPojos(pojos.flatMap { it.trackPojos })
                                }
                            },
                            onPlayNextClick = {
                                withTempAlbums(selectedYoutubeAlbums) { pojos ->
                                    viewModel.playTrackPojosNext(pojos.flatMap { it.trackPojos }, context)
                                }
                            },
                            onUnselectAllClick = { viewModel.unselectAllYoutubeAlbums() },
                            onAddToPlaylistClick = {
                                withTempAlbums(selectedYoutubeAlbums) { pojos ->
                                    appCallbacks.onAddToPlaylistClick(Selection(tracks = pojos.flatMap { it.tracks }))
                                }
                            },
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
                                onPlayNextClick = withTempTrack(pojo) { viewModel.playTrackPojoNext(it, context) },
                                onAddToPlaylistClick = withTempTrack(pojo) {
                                    appCallbacks.onAddToPlaylistClick(Selection(trackPojo = it))
                                },
                                onLongClick = { viewModel.toggleSelectedYoutube(pojo) },
                                onTrackClick = withTempTrack(pojo) {
                                    if (selectedYoutubeAlbums.isNotEmpty()) viewModel.toggleSelectedYoutube(it)
                                    else viewModel.playTrackPojo(it)
                                },
                            )
                        },
                        trackSelectionCallbacks = TrackSelectionCallbacks(
                            onAddToPlaylistClick = withTempTracks(selectedYoutubeTracks) {
                                appCallbacks.onAddToPlaylistClick(Selection(trackPojos = it))
                            },
                            onPlayClick = withTempTracks(selectedYoutubeTracks) { viewModel.playTrackPojos(it) },
                            onPlayNextClick = withTempTracks(selectedYoutubeTracks) {
                                viewModel.playTrackPojosNext(it, context)
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
    albumCallbacks: (Album) -> AlbumCallbacks,
    albumSelectionCallbacks: AlbumSelectionCallbacks,
    albums: List<AlbumPojo>,
    displayType: DisplayType,
    isSearching: Boolean,
    selectedAlbums: List<Album>,
) {
    when (displayType) {
        DisplayType.LIST -> {
            AlbumList(
                pojos = albums,
                albumCallbacks = albumCallbacks,
                albumSelectionCallbacks = albumSelectionCallbacks,
                selectedAlbums = selectedAlbums,
                onEmpty = {
                    if (!isSearching) Text(stringResource(R.string.no_albums_found), modifier = Modifier.padding(10.dp))
                },
            )
        }
        DisplayType.GRID -> {
            AlbumGrid(
                albums = albums,
                albumCallbacks = albumCallbacks,
                selectedAlbums = selectedAlbums,
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
