package us.huseli.thoucylinder.viewmodels

import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.uistates.TrackUiState
import us.huseli.thoucylinder.dataclasses.views.TrackCombo
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class RootStateViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
) : DownloadsViewModel(repos, managers) {
    private val _addToPlaylistTrackIds = MutableStateFlow<ImmutableList<String>>(persistentListOf())
    private val _albumToDownload = MutableStateFlow<Album?>(null)
    private val _createPlaylistActive = MutableStateFlow(false)
    private val _deleteAlbums = MutableStateFlow<ImmutableList<String>>(persistentListOf())
    private val _editAlbumId = MutableStateFlow<String?>(null)
    private val _editTrackState = MutableStateFlow<TrackUiState?>(null)
    private val _showInfoTrackCombo = MutableStateFlow<TrackCombo?>(null)
    private val _showLibraryRadioDialog = MutableStateFlow(false)

    val addToPlaylistTrackIds = _addToPlaylistTrackIds.asStateFlow()
    val albumToDownload = _albumToDownload.asStateFlow()
    val createPlaylistActive = _createPlaylistActive.asStateFlow()
    val deleteAlbums = _deleteAlbums.asStateFlow()
    val editAlbumId = _editAlbumId.asStateFlow()
    val editTrackState = _editTrackState.asStateFlow()
    val showInfoTrackCombo = _showInfoTrackCombo.asStateFlow()
    val showLibraryRadioDialog = _showLibraryRadioDialog.asStateFlow()

    fun createPlaylist(name: String, onFinish: (String) -> Unit) {
        launchOnIOThread {
            val playlist = Playlist(name = name)
            repos.playlist.insertPlaylist(playlist)
            onFinish(playlist.playlistId)
        }
    }

    fun setAlbumToDownloadId(albumId: String?) {
        launchOnIOThread { _albumToDownload.value = albumId?.let { repos.album.getAlbum(it) } }
    }

    fun setAddToPlaylistAlbumIds(albumIds: Collection<String>) {
        launchOnIOThread { _addToPlaylistTrackIds.value = repos.track.listTrackIdsByAlbumId(albumIds) }
    }

    fun setAddToPlaylistTrackIds(trackIds: Collection<String>) {
        _addToPlaylistTrackIds.value = trackIds.toImmutableList()
    }

    fun setCreatePlaylistActive(value: Boolean) {
        _createPlaylistActive.value = value
    }

    fun setDeleteAlbums(albumIds: Collection<String>) {
        _deleteAlbums.value = albumIds.toImmutableList()
    }

    fun setEditAlbumId(albumId: String?) {
        _editAlbumId.value = albumId
    }

    fun setEditTrackId(trackId: String?) {
        launchOnIOThread {
            _editTrackState.value =
                trackId?.let { repos.track.getTrackComboById(it)?.let { combo -> TrackUiState.fromTrackCombo(combo) } }
        }
    }

    fun setLocalMusicUri(value: Uri) = repos.settings.setLocalMusicUri(value)

    fun setShowInfoTrackCombo(trackId: String?) {
        launchOnIOThread {
            _showInfoTrackCombo.value = trackId?.let {
                repos.track.getTrackComboById(trackId)?.let { combo ->
                    combo.copy(
                        track = managers.library.ensureTrackMetadata(combo.track),
                        localPath = repos.track.getLocalAbsolutePath(combo.track),
                    )
                }
            }
        }
    }

    fun setShowLibraryRadioDialog(value: Boolean) {
        _showLibraryRadioDialog.value = value
    }
}
