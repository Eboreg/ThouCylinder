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
import us.huseli.thoucylinder.dataclasses.album.AlbumUiState
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
        repos.album.listAlbumCombos(albumIds).map { it.toUiState() }.toImmutableList()
    }.distinctUntilChanged().stateLazily(persistentListOf())
    val isImportingLocalMedia = repos.localMedia.isImportingLocalMedia

    fun deleteLocalAlbumFiles() {
        launchOnIOThread {
            val firstTitle = albumUiStates.value.first().title

            managers.library.deleteLocalAlbumFiles(_albumIds.value)
            repos.message.onDeleteLocalAlbumFiles(
                albumCount = _albumIds.value.size,
                firstTitle = firstTitle,
            )
        }
    }

    fun hideAlbums(
        onGotoLibraryClick: (() -> Unit)? = null,
        onGotoAlbumClick: ((String) -> Unit)? = null,
    ) {
        launchOnIOThread {
            val firstTitle = albumUiStates.value.first().title

            repos.album.setAlbumsIsHidden(_albumIds.value, true)
            repos.message.onHideAlbums(
                albumCount = _albumIds.value.size,
                firstTitle = firstTitle,
                onUndoClick = { reAddAlbumsToLibrary(onGotoLibraryClick, onGotoAlbumClick) },
            )
        }
    }

    fun hideAlbumsAndDeleteFiles(
        onGotoLibraryClick: (() -> Unit)? = null,
        onGotoAlbumClick: ((String) -> Unit)? = null,
    ) {
        launchOnIOThread {
            val firstTitle = albumUiStates.value.first().title

            repos.album.setAlbumsIsHidden(_albumIds.value, true)
            managers.library.deleteLocalAlbumFiles(_albumIds.value)
            repos.message.onHideAlbumsAndDeleteFiles(
                albumCount = _albumIds.value.size,
                firstTitle = firstTitle,
                onUndoClick = { reAddAlbumsToLibrary(onGotoLibraryClick, onGotoAlbumClick) },
            )
        }
    }

    fun setAlbumIds(albumIds: Collection<String>) {
        _albumIds.value = albumIds.toList()
    }

    private fun reAddAlbumsToLibrary(
        onGotoLibraryClick: (() -> Unit)? = null,
        onGotoAlbumClick: ((String) -> Unit)? = null,
    ) {
        launchOnIOThread {
            managers.library.addAlbumsToLibrary(
                albumIds = _albumIds.value,
                onGotoLibraryClick = onGotoLibraryClick,
                onGotoAlbumClick = onGotoAlbumClick,
            )
        }
    }
}
