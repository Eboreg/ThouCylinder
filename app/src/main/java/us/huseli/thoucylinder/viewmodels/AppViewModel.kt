package us.huseli.thoucylinder.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistPojo
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistTrackPojo
import us.huseli.thoucylinder.repositories.Repositories
import us.huseli.thoucylinder.size
import us.huseli.thoucylinder.toBitmap
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel(repos) {
    private var albumDownloadJobs = mutableMapOf<UUID, Job>()
    private var deletedPlaylist: PlaylistPojo? = null
    private var deletedPlaylistTracks: List<PlaylistTrackPojo> = emptyList()

    val playlists = repos.room.playlists

    fun addSelectionToPlaylist(selection: Selection, playlist: PlaylistPojo) =
        viewModelScope.launch {
            repos.room.addSelectionToPlaylist(selection, playlist, playlist.trackCount)
        }

    fun cancelAlbumDownload(albumId: UUID) = albumDownloadJobs[albumId]?.cancel()

    fun createPlaylist(playlist: Playlist, selection: Selection? = null) = viewModelScope.launch {
        repos.room.insertPlaylist(playlist, selection)
    }

    fun deleteOrphanTracksAndAlbums() = viewModelScope.launch {
        val allTracks = repos.room.listTracks()
        val albums = repos.room.listAlbums()
        val albumMultimap = albums.associateWith { album -> allTracks.filter { it.albumId == album.albumId } }
        // Collect tracks that have no Youtube connection and no existing media files:
        val orphanTracks = repos.mediaStore.listOrphanTracks(allTracks)
        // And albums that _only_ have orphan tracks in them:
        val orphanAlbums = albumMultimap
            .filter { (_, tracks) -> orphanTracks.map { it.trackId }.containsAll(tracks.map { it.trackId }) }
            .map { it.key }

        repos.room.deleteTracks(orphanTracks)
        repos.room.deleteAlbums(orphanAlbums)
    }

    fun deletePlaylist(pojo: PlaylistPojo, onFinish: (() -> Unit)? = null) = viewModelScope.launch {
        deletedPlaylist = pojo
        deletedPlaylistTracks = repos.room.listPlaylistTracks(pojo.playlistId)
        repos.room.deletePlaylist(pojo.toPlaylist())
        onFinish?.invoke()
    }

    fun deleteTempTracksAndAlbums() = viewModelScope.launch { repos.room.deleteTempTracksAndAlbums() }

    fun downloadAndSaveAlbum(pojo: AlbumWithTracksPojo) {
        albumDownloadJobs[pojo.album.albumId] = viewModelScope.launch {
            try {
                val tracks = repos.youtube.downloadTracks(
                    tracks = pojo.tracks,
                    progressCallback = { repos.youtube.setAlbumDownloadProgress(pojo.album.albumId, it) },
                )
                val newPojo = repos.mediaStore.moveTaggedAlbumToMediaStore(
                    pojo = pojo.copy(tracks = tracks, album = pojo.album.copy(isLocal = true)),
                    progressCallback = { repos.youtube.setAlbumDownloadProgress(pojo.album.albumId, it) },
                )
                repos.youtube.setAlbumDownloadProgress(pojo.album.albumId, null)
                repos.room.saveAlbumWithTracks(newPojo)
            } catch (e: Exception) {
                Log.e("download", e.toString(), e)
            } finally {
                repos.youtube.setAlbumDownloadProgress(pojo.album.albumId, null)
                albumDownloadJobs -= pojo.album.albumId
            }
        }
    }

    fun downloadTrack(track: Track) = viewModelScope.launch {
        try {
            var newTrack = repos.youtube.downloadTrack(
                track = track,
                progressCallback = {
                    repos.youtube.setTrackDownloadProgress(track.trackId, it.copy(progress = it.progress * 0.8))
                }
            )
            newTrack = repos.mediaStore.moveTaggedTrackToMediaStore(newTrack) {
                repos.youtube.setTrackDownloadProgress(track.trackId, it.copy(progress = 0.8 + (it.progress * 0.2)))
            }
            withContext(Dispatchers.Main) { repos.room.insertTrack(newTrack) }
        } catch (e: Exception) {
            Log.e("downloadTrack", e.toString(), e)
        } finally {
            repos.youtube.setTrackDownloadProgress(track.trackId, null)
        }
    }

    suspend fun getTrackAlbum(track: Track): Album? = repos.room.getTrackAlbum(track)

    fun importNewMediaStoreAlbums(context: Context) = viewModelScope.launch {
        val existingTracks = repos.room.listTracks()
        val newAlbums = repos.mediaStore.listNewMediaStoreAlbums(existingTracks)

        newAlbums.forEach { pojo ->
            val bestBitmap = repos.mediaStore.collectAlbumImages(pojo)
                .mapNotNull { it.toBitmap() }
                .maxByOrNull { it.size() }
            val mediaStoreImage = bestBitmap?.let {
                MediaStoreImage.fromBitmap(bitmap = it, album = pojo.album, context = context)
            }

            repos.room.saveAlbumWithTracks(pojo.copy(album = pojo.album.copy(albumArt = mediaStoreImage)))
        }
    }

    fun saveAlbumWithTracks(pojo: AlbumWithTracksPojo) = viewModelScope.launch {
        repos.room.saveAlbumWithTracks(ensureTrackMetadata(pojo))
    }

    fun tagAlbumTracks(pojo: AlbumWithTracksPojo) = viewModelScope.launch {
        repos.mediaStore.tagAlbumTracks(ensureTrackMetadata(pojo))
    }

    fun undoDeletePlaylist(onFinish: (PlaylistPojo) -> Unit) = viewModelScope.launch {
        deletedPlaylist?.also { pojo ->
            repos.room.insertPlaylist(pojo, deletedPlaylistTracks)
            deletedPlaylist = null
            deletedPlaylistTracks = emptyList()
            onFinish(pojo)
        }
    }

    private suspend fun ensureTrackMetadata(pojo: AlbumWithTracksPojo): AlbumWithTracksPojo = pojo.copy(
        tracks = pojo.tracks.map { track -> ensureTrackMetadata(track) }
    )
}
