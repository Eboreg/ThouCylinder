package us.huseli.thoucylinder.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import us.huseli.thoucylinder.dataclasses.views.ArtistCombo
import us.huseli.thoucylinder.enums.ArtistSortParameter
import us.huseli.thoucylinder.enums.SortOrder
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class ArtistListViewModel @Inject constructor(repos: Repositories) : AbstractBaseViewModel() {
    private val _isLoading = MutableStateFlow(true)
    private val _onlyShowArtistsWithAlbums = MutableStateFlow(false)
    private val _searchTerm = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.ASCENDING)
    private val _sortParameter = MutableStateFlow(ArtistSortParameter.NAME)

    val artistCombos: StateFlow<ImmutableList<ArtistCombo>> = combine(
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
            .toImmutableList()
    }
        .onEach { _isLoading.value = false }
        .stateLazily(persistentListOf())

    val isLoading = _isLoading.asStateFlow()
    val onlyShowArtistsWithAlbums = _onlyShowArtistsWithAlbums.asStateFlow()
    val searchTerm = _searchTerm.asStateFlow()
    val sortOrder = _sortOrder.asStateFlow()
    val sortParameter = _sortParameter.asStateFlow()

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
