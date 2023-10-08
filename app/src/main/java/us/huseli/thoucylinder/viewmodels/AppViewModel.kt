package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.dataclasses.entities.AbstractPlaylist
import us.huseli.thoucylinder.repositories.LocalRepository
import us.huseli.thoucylinder.repositories.MediaStoreRepository
import us.huseli.thoucylinder.repositories.PlayerRepository
import us.huseli.thoucylinder.repositories.YoutubeRepository
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repo: LocalRepository,
    playerRepo: PlayerRepository,
    youtubeRepo: YoutubeRepository,
    private val mediaStoreRepo: MediaStoreRepository,
) : BaseViewModel(repo, playerRepo, youtubeRepo, mediaStoreRepo) {
    val playerCurrentPositionMs = playerRepo.currentPositionMs
    val playlists = repo.playlists

    fun addSelectionToPlaylist(selection: Selection, playlist: AbstractPlaylist) =
        viewModelScope.launch(Dispatchers.IO) {
            repo.addSelectionToPlaylist(selection, playlist)
        }

    fun deleteOrphanTracksAndAlbums() = viewModelScope.launch(Dispatchers.IO) {
        val allTracks = repo.listTracks()
        val albums = repo.listAlbums()
        val albumMultimap = albums.associateWith { album -> allTracks.filter { it.albumId == album.albumId } }
        // Collect tracks that have no Youtube connection and no existing media files:
        val orphanTracks = mediaStoreRepo.listOrphanTracks(allTracks)
        // And albums that _only_ have orphan tracks in them:
        val orphanAlbums = albumMultimap
            .filter { (_, tracks) -> orphanTracks.map { it.trackId }.containsAll(tracks.map { it.trackId }) }
            .map { it.key }

        repo.deleteTracks(orphanTracks)
        repo.deleteAlbums(orphanAlbums)
    }

    fun importNewMediaStoreAlbums() = viewModelScope.launch(Dispatchers.IO) {
        val existingTracks = repo.listTracks()
        val newAlbums = mediaStoreRepo.listNewMediaStoreAlbums(existingTracks)

        newAlbums.forEach { repo.saveAlbumWithTracks(it) }
    }
}
