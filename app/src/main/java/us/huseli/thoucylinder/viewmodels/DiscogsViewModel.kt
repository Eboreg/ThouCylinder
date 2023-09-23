package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.dataclasses.DiscogsMasterData
import us.huseli.thoucylinder.dataclasses.DiscogsSearchResultItem
import us.huseli.thoucylinder.dataclasses.TempAlbum
import us.huseli.thoucylinder.repositories.DiscogsRepository
import javax.inject.Inject

@HiltViewModel
class DiscogsViewModel @Inject constructor(private val repo: DiscogsRepository) : ViewModel() {
    private val _tempAlbum = MutableStateFlow<TempAlbum?>(null)

    val searchResults: Flow<List<DiscogsSearchResultItem>> = _tempAlbum.map { album ->
        album?.let { repo.searchMasters(it.title, it.artist)?.data?.results } ?: emptyList()
    }

    fun getMaster(masterId: Int?) = MutableStateFlow<DiscogsMasterData?>(null).apply {
        viewModelScope.launch {
            value = masterId?.let { repo.getMaster(it)?.data }
        }
    }.asStateFlow()

    fun setTempAlbum(value: TempAlbum) {
        _tempAlbum.value = value
    }
}
