package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.dataclasses.abstr.AbstractPlaylist
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(private val repos: Repositories) : BaseViewModel(repos) {
    val playlists = repos.local.playlists

    fun addSelectionToPlaylist(selection: Selection, playlist: AbstractPlaylist) =
        viewModelScope.launch(Dispatchers.IO) {
            repos.local.addSelectionToPlaylist(selection, playlist)
        }

    fun deleteOrphanTracksAndAlbums() = viewModelScope.launch(Dispatchers.IO) {
        val allTracks = repos.local.listTracks()
        val albums = repos.local.listAlbums()
        val albumMultimap = albums.associateWith { album -> allTracks.filter { it.albumId == album.albumId } }
        // Collect tracks that have no Youtube connection and no existing media files:
        val orphanTracks = repos.mediaStore.listOrphanTracks(allTracks)
        // And albums that _only_ have orphan tracks in them:
        val orphanAlbums = albumMultimap
            .filter { (_, tracks) -> orphanTracks.map { it.trackId }.containsAll(tracks.map { it.trackId }) }
            .map { it.key }

        repos.local.deleteTracks(orphanTracks)
        repos.local.deleteAlbums(orphanAlbums)
    }

    fun importNewMediaStoreAlbums() = viewModelScope.launch(Dispatchers.IO) {
        val existingTracks = repos.local.listTracks()
        val newAlbums = repos.mediaStore.listNewMediaStoreAlbums(existingTracks)

        newAlbums.forEach { repos.local.saveAlbumWithTracks(it) }
    }
}
