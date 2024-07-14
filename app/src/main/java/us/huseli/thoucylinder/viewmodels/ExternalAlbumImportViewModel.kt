package us.huseli.thoucylinder.viewmodels

import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import us.huseli.retaintheme.extensions.launchOnMainThread
import us.huseli.thoucylinder.dataclasses.ProgressData
import us.huseli.thoucylinder.dataclasses.album.ImportableAlbumUiState
import us.huseli.thoucylinder.dataclasses.track.TrackUiState
import us.huseli.thoucylinder.externalcontent.IExternalImportBackend
import us.huseli.thoucylinder.externalcontent.ImportBackend
import us.huseli.thoucylinder.externalcontent.holders.AbstractAlbumImportHolder
import us.huseli.thoucylinder.interfaces.IExternalAlbumWithTracks
import us.huseli.thoucylinder.interfaces.ILogger
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExternalAlbumImportViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractAlbumListViewModel<ImportableAlbumUiState>("ExternalAlbumImportViewModel2", repos, managers), ILogger {
    private val _backendKey = MutableStateFlow(ImportBackend.LOCAL)
    private val _backend = _backendKey.mapLatest { managers.external.getImportBackend(it) }

    private val _holder: Flow<AbstractAlbumImportHolder<out IExternalAlbumWithTracks>> =
        _backend.mapLatest { it.albumImportHolder }

    private val currentBackend: IExternalImportBackend<*>
        get() = managers.external.getImportBackend(_backendKey.value)

    override val baseAlbumUiStates: StateFlow<ImmutableList<ImportableAlbumUiState>> = _holder
        .flatMapLatest { it.currentPageItems.map { states -> states.toImmutableList() } }
        .stateWhileSubscribed(persistentListOf())
    override val baseTrackUiStates: StateFlow<ImmutableList<TrackUiState>> = MutableStateFlow(persistentListOf())
    override val selectedAlbumIds: Flow<List<String>> = _holder.flatMapLatest { it.selectedItemIds }

    val backendKey = _backendKey.asStateFlow()
    val canImport = _holder.flatMapLatest { it.canImport }.stateWhileSubscribed(false)
    val currentAlbumCount = baseAlbumUiStates.map { it.size }.stateWhileSubscribed(0)
    val displayOffset = _holder.flatMapLatest { it.displayOffset }.stateWhileSubscribed(0)
    val hasNextPage = _holder.flatMapLatest { it.hasNextPage }.stateWhileSubscribed(false)
    val hasPreviousPage =
        _holder.flatMapLatest { holder -> holder.currentPage.map { it > 0 } }.stateWhileSubscribed(false)
    val isAllSelected = _holder.flatMapLatest { it.isWholeCurrentPageSelected }.stateWhileSubscribed(false)
    val isEmpty = _holder.flatMapLatest { it.isEmpty }.stateWhileSubscribed(false)
    val isLoadingCurrentPage = _holder.flatMapLatest { it.isLoadingCurrentPage }.stateWhileSubscribed(false)
    val isSelectAllEnabled: StateFlow<Boolean> = _holder.flatMapLatest { it.canSelectAll }.stateWhileSubscribed(false)
    val isTotalAlbumCountExact: StateFlow<Boolean> =
        _holder.flatMapLatest { it.isTotalCountExact }.stateWhileSubscribed(false)
    val progress = managers.external.albumImportProgress.stateWhileSubscribed(ProgressData())
    val searchTerm = _holder.flatMapLatest { it.searchTerm }.stateWhileSubscribed("")
    val totalItemCount = _holder.flatMapLatest { it.totalItemCount }.stateWhileSubscribed(0)

    val isImportButtonEnabled =
        _holder.flatMapLatest { it.selectedItemIds.map { ids -> ids.isNotEmpty() } }.stateWhileSubscribed(false)

    fun getSpotifyAuthUrl() = repos.spotify.oauth2PKCE.getAuthUrl()

    fun gotoNextPage() = currentBackend.albumImportHolder.gotoNextPage()

    fun gotoPreviousPage() = currentBackend.albumImportHolder.gotoPreviousPage()

    fun importSelectedAlbums(matchYoutube: Boolean) {
        val holder = currentBackend.albumImportHolder

        launchOnMainThread {
            val selectedStates = holder.getSelectedItems()

            for (state in selectedStates) {
                managers.external.enqueueAlbumImport(
                    state = state,
                    holder = holder,
                    matchYoutube = matchYoutube,
                )
            }
        }
    }

    fun initBackend() {
        currentBackend.albumImportHolder.start()
    }

    fun setBackend(value: ImportBackend) {
        _backendKey.value = value
    }

    fun setLocalImportUri(uri: Uri) = managers.external.setLocalImportUri(uri)

    fun setSearchTerm(value: String) = currentBackend.albumImportHolder.setSearchTerm(value)

    fun toggleSelectAll() {
        if (isAllSelected.value) currentBackend.albumImportHolder.deselectAll()
        else currentBackend.albumImportHolder.selectAll()
    }

    fun toggleSelected(albumId: String) = currentBackend.albumImportHolder.toggleItemSelected(albumId)

    fun unauthorizeSpotify() = repos.spotify.unauthorize()
}
