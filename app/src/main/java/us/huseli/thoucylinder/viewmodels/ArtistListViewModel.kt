package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import us.huseli.thoucylinder.ArtistSortParameter
import us.huseli.thoucylinder.SortOrder
import us.huseli.thoucylinder.asThumbnailImageBitmap
import us.huseli.thoucylinder.dataclasses.views.ArtistCombo
import us.huseli.thoucylinder.getBitmap
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class ArtistListViewModel @Inject constructor(private val repos: Repositories) : ViewModel() {
    private val _isLoading = MutableStateFlow(true)
    private val _onlyShowArtistsWithAlbums = MutableStateFlow(false)
    private val _searchTerm = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.ASCENDING)
    private val _sortParameter = MutableStateFlow(ArtistSortParameter.NAME)

    val artistCombos = combine(
        repos.artist.artistCombos,
        _searchTerm,
        _sortParameter,
        _sortOrder,
        _onlyShowArtistsWithAlbums,
    ) { artistCombos, searchTerm, sortParameter, sortOrder, onlyWithAlbums ->
        artistCombos
            .filter { it.artist.name.contains(searchTerm, true) }
            .filter { if (onlyWithAlbums) it.albumCount > 0 else true }
            .let { combos ->
                when (sortParameter) {
                    ArtistSortParameter.NAME -> combos.sortedBy { it.artist.name.lowercase() }
                    ArtistSortParameter.ALBUM_COUNT -> combos.sortedBy { it.albumCount }
                    ArtistSortParameter.TRACK_COUNT -> combos.sortedBy { it.trackCount }
                }
            }
            .let { if (sortOrder == SortOrder.DESCENDING) it.reversed() else it }
    }.onEach { _isLoading.value = false }
    val isLoading = _isLoading.asStateFlow()
    val onlyShowArtistsWithAlbums = _onlyShowArtistsWithAlbums.asStateFlow()
    val searchTerm = _searchTerm.asStateFlow()
    val sortOrder = _sortOrder.asStateFlow()
    val sortParameter = _sortParameter.asStateFlow()

    suspend fun getArtistImage(combo: ArtistCombo, context: Context) = withContext(Dispatchers.IO) {
        repos.artist.artistImageUriCache.getOrNull(combo)?.getBitmap(context)?.asThumbnailImageBitmap(context)
    }

    fun setOnlyShowArtistsWithAlbums(value: Boolean) {
        _onlyShowArtistsWithAlbums.value = value
    }

    fun setSearchTerm(value: String) {
        _searchTerm.value = value
    }

    fun setSorting(sortParameter: ArtistSortParameter, sortOrder: SortOrder) {
        _sortParameter.value = sortParameter
        _sortOrder.value = sortOrder
    }
}
