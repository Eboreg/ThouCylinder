package us.huseli.thoucylinder.viewmodels

import android.content.Context
import android.os.Environment
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.retaintheme.toBitmap
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistPojo
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistTrackPojo
import us.huseli.thoucylinder.dataclasses.pojos.QueueTrackPojo
import us.huseli.thoucylinder.repositories.PlayerRepository
import us.huseli.thoucylinder.repositories.PlayerRepositoryListener
import us.huseli.thoucylinder.repositories.Repositories
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel(repos), PlayerRepositoryListener {
    private var albumDownloadJobs = mutableMapOf<UUID, Job>()
    private var deletedPlaylist: PlaylistPojo? = null
    private var deletedPlaylistTracks: List<PlaylistTrackPojo> = emptyList()

    val playlists = repos.room.playlists

    init {
        repos.player.addListener(this)
    }

    fun addSelectionToPlaylist(selection: Selection, playlist: PlaylistPojo) =
        viewModelScope.launch {
            repos.room.addSelectionToPlaylist(selection, playlist, playlist.trackCount)
        }

    fun cancelAlbumDownload(albumId: UUID) = albumDownloadJobs[albumId]?.cancel()

    fun createPlaylist(playlist: Playlist, selection: Selection? = null) = viewModelScope.launch {
        repos.room.insertPlaylist(playlist, selection)
    }

    fun deleteAlbumAndFiles(album: Album) = viewModelScope.launch {
        repos.room.getAlbumWithTracks(album.albumId)?.also { pojo ->
            repos.mediaStore.deleteImages(pojo)
            repos.mediaStore.deleteTracks(pojo.tracks)
            repos.room.deleteAlbumWithTracks(pojo.album)
        }
    }

    fun deleteOrphanTracksAndAlbums() = viewModelScope.launch {
        val allTracks = repos.room.listTracks()
        val albums = repos.room.listAlbums()
        val albumMultimap = albums.associateWith { album -> allTracks.filter { it.albumId == album.albumId } }
        // Collect tracks that have no existing media files:
        val orphanTracks = repos.mediaStore.listOrphanTracks(allTracks)
        // Separate those that have Youtube connection from those that don't:
        val (realOrphanTracks, youtubeOnlyTracks) = orphanTracks.partition { it.youtubeVideo == null }
        // And albums that _only_ have orphan tracks in them:
        val realOrphanAlbums = albumMultimap
            .filter { (_, tracks) -> realOrphanTracks.map { it.trackId }.containsAll(tracks.map { it.trackId }) }
            .keys
        val youtubeOnlyAlbums = albumMultimap
            .filter { (_, tracks) -> youtubeOnlyTracks.map { it.trackId }.containsAll(tracks.map { it.trackId }) }
            .keys

        // Delete the totally orphaned tracks and albums:
        repos.mediaStore.deleteImagesByTracks(realOrphanTracks)
        repos.mediaStore.deleteImagesByAlbums(realOrphanAlbums)
        repos.room.deleteTracks(realOrphanTracks)
        repos.room.deleteAlbums(realOrphanAlbums)
        // Update the Youtube-only tracks and albums:
        repos.room.updateAlbums(youtubeOnlyAlbums.map { it.copy(isLocal = false) })
        repos.room.updateTracks(youtubeOnlyTracks.map { it.copy(mediaStoreData = null) })
    }

    fun deletePlaylist(pojo: PlaylistPojo, onFinish: (() -> Unit)? = null) = viewModelScope.launch {
        deletedPlaylist = pojo
        deletedPlaylistTracks = repos.room.listPlaylistTracks(pojo.playlistId)
        repos.room.deletePlaylist(pojo.toPlaylist())
        onFinish?.invoke()
    }

    fun deleteTempTracksAndAlbums() = viewModelScope.launch { repos.room.deleteTempTracksAndAlbums() }

    fun deleteTrackFiles(album: Album) = viewModelScope.launch {
        repos.room.getAlbumWithTracks(album.albumId)?.also { pojo ->
            repos.mediaStore.deleteTracks(pojo.tracks)
            repos.room.updateTracks(pojo.tracks.map { it.copy(mediaStoreData = null) })
            repos.room.updateAlbum(pojo.album.copy(isLocal = false))
        }
    }

    fun downloadAndSaveAlbum(album: Album, onError: (Track, Throwable) -> Unit) = viewModelScope.launch {
        repos.room.getAlbumWithTracks(album.albumId)?.also { downloadAndSaveAlbum(it, onError) }
    }

    fun downloadAndSaveAlbum(pojo: AlbumWithTracksPojo, onError: (Track, Throwable) -> Unit) {
        albumDownloadJobs[pojo.album.albumId] = viewModelScope.launch {
            val tracks = mutableListOf<Track>()

            try {
                pojo.tracks.map { repos.youtube.ensureVideoMetadata(it) }.forEachIndexed { index, track ->
                    try {
                        val tempFile = repos.youtube.downloadVideo(
                            video = track.youtubeVideo!!,
                            progressCallback = { downloadProgress ->
                                repos.youtube.setAlbumDownloadProgress(
                                    pojo.album.albumId,
                                    downloadProgress.copy(
                                        progress = (index + (downloadProgress.progress * 0.9)) / pojo.tracks.size
                                    )
                                )
                            }
                        )
                        tracks.add(
                            repos.mediaStore.moveTaggedTrackToMediaStore(
                                track = track,
                                tempFile = tempFile,
                                relativePath = "${Environment.DIRECTORY_MUSIC}/${pojo.album.getMediaStoreSubdir()}",
                                album = pojo.album,
                                progressCallback = { downloadProgress ->
                                    repos.youtube.setAlbumDownloadProgress(
                                        pojo.album.albumId,
                                        downloadProgress.copy(
                                            progress = (index + downloadProgress.progress) / pojo.tracks.size
                                        )
                                    )
                                },
                            )
                        )
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        onError(track, e)
                    }
                }

                repos.room.saveAlbumWithTracks(pojo.copy(tracks = tracks, album = pojo.album.copy(isLocal = true)))
            } finally {
                repos.youtube.setAlbumDownloadProgress(pojo.album.albumId, null)
                albumDownloadJobs -= pojo.album.albumId
            }
        }
    }

    fun downloadTrack(track: Track) = viewModelScope.launch {
        try {
            var newTrack = repos.youtube.ensureVideoMetadata(track)
            val tempFile = repos.youtube.downloadVideo(
                video = newTrack.youtubeVideo!!,
                progressCallback = {
                    repos.youtube.setTrackDownloadProgress(track.trackId, it.copy(progress = it.progress * 0.8))
                },
            )
            newTrack = repos.mediaStore.moveTaggedTrackToMediaStore(
                track = newTrack,
                tempFile = tempFile,
                relativePath = Environment.DIRECTORY_MUSIC,
                progressCallback = {
                    repos.youtube.setTrackDownloadProgress(track.trackId, it.copy(progress = it.progress * 0.8))
                }
            )
            withContext(Dispatchers.Main) { repos.room.insertTrack(newTrack) }
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
                .maxByOrNull { it.width * it.height }
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

    override fun onPlayerError(
        error: PlaybackException,
        currentPojo: QueueTrackPojo?,
        lastAction: PlayerRepository.LastAction,
    ) = viewModelScope.launch {
        val metadataIsOld = currentPojo?.track?.youtubeVideo?.metadata?.expiresAt?.isBefore(Instant.now())

        if (
            error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS &&
            currentPojo != null &&
            (currentPojo.track.youtubeVideo?.metadata == null || metadataIsOld == true)
        ) {
            val track = repos.youtube.ensureVideoMetadata(currentPojo.track, forceReload = true)
            val playUri = track.playUri

            if (playUri != null && track.playUri != currentPojo.uri) {
                repos.room.updateTrack(track)
                repos.player.updateTrack(currentPojo.copy(track = track, uri = playUri))
                if (repos.player.lastAction == PlayerRepository.LastAction.PLAY) repos.player.play(currentPojo.position)
            }
        } else SnackbarEngine.addError(error.toString())
    }
}
