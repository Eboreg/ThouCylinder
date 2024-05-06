package us.huseli.thoucylinder.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.thoucylinder.dataclasses.uistates.AlbumUiState
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class DeleteAlbumsViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractBaseViewModel() {
    private val _albumIds = MutableStateFlow<List<String>>(emptyList())

    val albumUiStates: StateFlow<ImmutableList<AlbumUiState>> = _albumIds.map { albumIds ->
        repos.album.listAlbumCombos(albumIds).map { AlbumUiState.fromAlbumCombo(it) }.toImmutableList()
    }.distinctUntilChanged().stateLazily(persistentListOf())

    fun deleteLocalAlbumFiles(onFinish: () -> Unit = {}) {
        launchOnIOThread {
            managers.library.deleteLocalAlbumFiles(_albumIds.value)
            onFinish()
        }
    }

    fun hideAlbums(onFinish: () -> Unit = {}) {
        launchOnIOThread {
            repos.album.setAlbumsIsHidden(_albumIds.value, true)
            onFinish()
        }
    }

    fun hideAlbumsAndDeleteFiles(onFinish: () -> Unit = {}) {
        launchOnIOThread {
            repos.album.setAlbumsIsHidden(_albumIds.value, true)
            deleteLocalAlbumFiles(onFinish)
        }
    }

    fun reAddAlbumsToLibrary() {
        launchOnIOThread {
            repos.album.addAlbumsToLibrary(_albumIds.value)
            repos.track.addToLibraryByAlbumId(_albumIds.value)
        }
    }

    fun removeAlbumsFromLibrary(onFinish: () -> Unit = {}) {
        launchOnIOThread {
            repos.album.removeAlbumsFromLibrary(_albumIds.value)
            repos.track.removeFromLibraryByAlbumId(_albumIds.value)
            onFinish()
        }
    }

    fun setAlbumIds(albumIds: Collection<String>) {
        _albumIds.value = albumIds.toList()
    }

    fun unhideAlbums() {
        launchOnIOThread { repos.album.setAlbumsIsHidden(_albumIds.value, false) }
    }
}
