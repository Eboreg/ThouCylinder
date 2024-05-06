package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.uistates.AlbumUiState
import us.huseli.thoucylinder.dataclasses.uistates.TrackUiState
import us.huseli.thoucylinder.dataclasses.views.toAlbumArtists
import us.huseli.thoucylinder.dataclasses.views.toTrackArtists
import us.huseli.thoucylinder.getAlbumUiStateFlow
import us.huseli.thoucylinder.getTrackUiStateFlow
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class YoutubeSearchViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractAlbumListViewModel("YoutubeSearchViewModel", repos, managers) {
    private val _isSearchingAlbums = MutableStateFlow(false)
    private val _query = MutableStateFlow("")
    private val _albumCombos = MutableStateFlow<ImmutableList<AlbumWithTracksCombo>>(persistentListOf())
    private val _trackUiStates = MutableStateFlow<PagingData<TrackUiState>>(PagingData.empty())

    override val albumUiStates = combine(
        _albumCombos,
        managers.library.albumDownloadTasks,
    ) { combos, tasks ->
        combos.map { combo ->
            AlbumUiState.fromAlbumCombo(combo).copy(downloadState = tasks.getAlbumUiStateFlow(combo.album.albumId))
        }.toImmutableList()
    }.distinctUntilChanged().stateLazily(persistentListOf())

    val trackUiStates: StateFlow<PagingData<TrackUiState>> = _trackUiStates.asStateFlow()
    val isSearchingTracks: StateFlow<Boolean> = repos.youtube.isSearchingTracks
    val isSearchingAlbums: StateFlow<Boolean> = _isSearchingAlbums.asStateFlow()
    val query: StateFlow<String> = _query.asStateFlow()

    fun enqueueAlbum(albumId: String) = managers.player.enqueueAlbums(listOf(albumId))

    fun ensureTrackMetadata(uiState: TrackUiState) = managers.library.ensureTrackMetadataAsync(uiState.trackId)

    fun playAlbum(albumId: String) = managers.player.playAlbums(listOf(albumId))

    fun search(query: String) {
        if (query != _query.value) {
            _query.value = query

            if (query.length >= 3) {
                _isSearchingAlbums.value = true

                launchOnIOThread {
                    val combos = repos.youtube.searchPlaylistCombos(query)
                        .map { playlistCombo ->
                            playlistCombo.toAlbumWithTracks(
                                isInLibrary = false,
                                isLocal = false,
                                getArtist = { repos.artist.artistCache.get(it) },
                            )
                        }

                    if (combos.isNotEmpty()) {
                        val tracks = combos.flatMap { it.trackCombos.map { trackCombo -> trackCombo.track } }
                        repos.album.upsertAlbumsAndTags(combos)
                        if (tracks.isNotEmpty()) repos.track.upsertTracks(tracks)
                        repos.artist.insertAlbumArtists(combos.flatMap { it.artists.toAlbumArtists() })
                        repos.artist.insertTrackArtists(
                            combos.flatMap { combo -> combo.trackCombos.flatMap { it.artists.toTrackArtists() } }
                        )
                    }
                    _albumCombos.value = combos.toImmutableList()
                    _isSearchingAlbums.value = false
                }

                launchOnIOThread {
                    combine(
                        repos.youtube.searchTracks(query).flow.cachedIn(viewModelScope),
                        managers.library.trackDownloadTasks,
                    ) { pagingData, tasks ->
                        pagingData.map { track ->
                            TrackUiState.fromTrack(track).copy(downloadState = tasks.getTrackUiStateFlow(track.trackId))
                        }
                    }
                }
            }
        }
    }

    fun updateFromMusicBrainz(albumId: String) = managers.library.updateAlbumFromMusicBrainz(albumId)

    override fun getAlbumSelectionCallbacks(appCallbacks: AppCallbacks, context: Context): AlbumSelectionCallbacks =
        super.getAlbumSelectionCallbacks(appCallbacks, context).copy(onDeleteClick = null)

    override fun onAllAlbumIds(callback: (Collection<String>) -> Unit) {
        callback(_albumCombos.value.map { it.album.albumId })
    }

    override fun onSelectedAlbumsWithTracks(callback: (Collection<AlbumWithTracksCombo>) -> Unit) {
        callback(_albumCombos.value.filter { selectedAlbumIds.value.contains(it.album.albumId) })
    }

    override fun onSelectedAlbumTracks(callback: (Collection<Track>) -> Unit) {
        callback(
            _albumCombos.value.filter { selectedAlbumIds.value.contains(it.album.albumId) }
                .flatMap { combo -> combo.trackCombos.map { it.track } }
        )
    }
}
