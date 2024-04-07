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
import kotlinx.coroutines.flow.collectLatest
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.views.TrackCombo
import us.huseli.thoucylinder.dataclasses.views.toAlbumArtists
import us.huseli.thoucylinder.dataclasses.views.toTrackArtists
import us.huseli.thoucylinder.launchOnIOThread
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class YoutubeSearchViewModel @Inject constructor(
    private val repos: Repositories,
) : AbstractAlbumListViewModel("YoutubeSearchViewModel", repos) {
    private val _isSearchingAlbums = MutableStateFlow(false)
    private val _query = MutableStateFlow("")
    private val _albumCombos = MutableStateFlow<ImmutableList<AlbumWithTracksCombo>>(persistentListOf())
    private val _trackCombos = MutableStateFlow<PagingData<TrackCombo>>(PagingData.empty())
    private val _albumViewStates = MutableStateFlow<ImmutableList<Album.ViewState>>(persistentListOf())

    override val albumViewStates = _albumViewStates.asStateFlow()

    val trackCombos: StateFlow<PagingData<TrackCombo>> = _trackCombos.asStateFlow()
    val isSearchingTracks: StateFlow<Boolean> = repos.youtube.isSearchingTracks
    val isSearchingAlbums: StateFlow<Boolean> = _isSearchingAlbums.asStateFlow()
    val query: StateFlow<String> = _query.asStateFlow()

    fun search(query: String) {
        if (query != _query.value) {
            _query.value = query

            if (query.length >= 3) {
                _isSearchingAlbums.value = true

                launchOnIOThread {
                    val combos = repos.youtube.searchPlaylistCombos(query)
                        .map { playlistCombo ->
                            playlistCombo.toAlbumCombo(
                                isInLibrary = false,
                                getArtist = { repos.artist.artistCache.getByName(it) },
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
                    _albumViewStates.value = combos.map { it.getViewState() }.toImmutableList()
                    _isSearchingAlbums.value = false
                }

                launchOnIOThread {
                    repos.youtube.searchTracks(query).flow.cachedIn(viewModelScope).collectLatest { pagingData ->
                        _trackCombos.value = pagingData.map { TrackCombo(track = it) }
                    }
                }
            }
        }
    }

    fun updateFromMusicBrainz(albumId: String) = launchOnIOThread {
        repos.album.getAlbumWithTracks(albumId)?.also { updateFromMusicBrainz(it) }
    }

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
