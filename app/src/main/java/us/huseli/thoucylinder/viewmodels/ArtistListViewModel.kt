package us.huseli.thoucylinder.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import us.huseli.thoucylinder.dataclasses.artist.ArtistUiState
import us.huseli.thoucylinder.enums.ArtistSortParameter
import us.huseli.thoucylinder.enums.SortOrder
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class ArtistListViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel() {
    private val _isLoading = MutableStateFlow(true)

    val artistUiStates: StateFlow<ImmutableList<ArtistUiState>> = combine(
        repos.artist.artistCombos,
        repos.settings.artistSearchTerm,
        repos.settings.artistSortParameter,
        repos.settings.artistSortOrder,
        repos.settings.showArtistsWithoutAlbums,
    ) { artistCombos, searchTerm, sortParameter, sortOrder, showWithoutAlbums ->
        artistCombos
            .filter { it.artist.name.contains(searchTerm, true) }
            .filter { if (!showWithoutAlbums) it.albumCount > 0 else true }
            .let { combos ->
                when (sortParameter) {
                    ArtistSortParameter.NAME -> combos.sortedBy { it.artist.name.lowercase() }
                    ArtistSortParameter.ALBUM_COUNT -> combos.sortedBy { it.albumCount }
                    ArtistSortParameter.TRACK_COUNT -> combos.sortedBy { it.trackCount }
                }
            }
            .let { if (sortOrder == SortOrder.DESCENDING) it.reversed() else it }
            .map { combo -> combo.toUiState() }
            .toImmutableList()
    }
        .onEach { _isLoading.value = false }
        .stateLazily(persistentListOf())

    val isEmpty: StateFlow<Boolean> =
        combine(artistUiStates, _isLoading, repos.localMedia.isImportingLocalMedia) { combos, isLoading, isImporting ->
            combos.isEmpty() && !isLoading && !isImporting
        }.distinctUntilChanged().stateLazily(false)

    val isLoading = _isLoading.asStateFlow()
    val searchTerm = repos.settings.artistSearchTerm
    val showArtistsWithoutAlbums = repos.settings.showArtistsWithoutAlbums
    val sortOrder = repos.settings.artistSortOrder
    val sortParameter = repos.settings.artistSortParameter

    fun setSearchTerm(value: String) = repos.settings.setArtistSearchTerm(value)

    fun setShowArtistsWithoutAlbums(value: Boolean) = repos.settings.setShowArtistsWithoutAlbums(value)

    fun setSorting(sortParameter: ArtistSortParameter, sortOrder: SortOrder) =
        repos.settings.setArtistSorting(sortParameter, sortOrder)
}
