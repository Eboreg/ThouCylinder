package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.Constants.NAV_ARG_ARTIST
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.views.TrackCombo
import us.huseli.thoucylinder.launchOnIOThread
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val repos: Repositories,
    savedStateHandle: SavedStateHandle,
) : AbstractAlbumListViewModel("ArtistViewModel", repos) {
    private val _displayType = MutableStateFlow(DisplayType.LIST)
    private val _listType = MutableStateFlow(ListType.ALBUMS)
    private val artistId: String = savedStateHandle.get<String>(NAV_ARG_ARTIST)!!

    override val albumViewStates: Flow<ImmutableList<Album.ViewState>> =
        repos.album.flowAlbumCombosByArtist(artistId)
            .map { combos -> combos.map { it.getViewState() }.toImmutableList() }

    val displayType = _displayType.asStateFlow()
    val listType = _listType.asStateFlow()
    val trackCombos: Flow<PagingData<TrackCombo>> =
        repos.track.pageTrackCombosByArtist(artistId).flow.cachedIn(viewModelScope)
    val artist: Flow<Artist?> = repos.artist.flowArtistById(artistId)

    fun enqueueArtist(context: Context) = launchOnIOThread {
        val queueTrackCombos = getQueueTrackCombos(repos.track.listTrackCombosByArtistId(artistId))

        if (queueTrackCombos.isNotEmpty()) {
            withContext(Dispatchers.Main) { repos.player.insertNext(queueTrackCombos) }
            SnackbarEngine.addInfo(
                context.resources.getQuantityString(
                    R.plurals.x_tracks_enqueued_next,
                    queueTrackCombos.size,
                    queueTrackCombos.size,
                )
            )
        }
    }

    fun onAllArtistTracks(callback: (ImmutableList<Track>) -> Unit) = launchOnIOThread {
        callback(repos.track.listTracksByArtistId(artistId).toImmutableList())
    }

    fun playArtist() = launchOnIOThread {
        val queueTrackCombos = getQueueTrackCombos(repos.track.listTrackCombosByArtistId(artistId))

        if (queueTrackCombos.isNotEmpty()) withContext(Dispatchers.Main) {
            repos.player.replaceAndPlay(queueTrackCombos)
        }
    }

    fun setDisplayType(value: DisplayType) {
        _displayType.value = value
    }

    fun setListType(value: ListType) {
        _listType.value = value
    }
}
