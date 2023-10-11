package us.huseli.thoucylinder.repositories

import androidx.compose.ui.graphics.ImageBitmap
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.pojos.AlbumPojo
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.Image
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import us.huseli.thoucylinder.dataclasses.abstr.AbstractPlaylist
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.PlaylistTrack
import us.huseli.thoucylinder.dataclasses.entities.Track
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class LocalRepository @Inject constructor(private val database: Database) {
    private val trackDao = database.trackDao()
    private val albumDao = database.albumDao()
    private val playlistDao = database.playlistDao()
    private val artistDao = database.artistDao()
    private val _tempAlbumPojos = MutableStateFlow<Map<UUID, AlbumWithTracksPojo>>(emptyMap())
    private val _imageCache = mutableMapOf<Image, ImageBitmap?>()
    private val _imageCacheMutex = Mutex()

    val albumPojos: Flow<List<AlbumPojo>> = albumDao.flowAlbumPojos()
    val artistPojos = artistDao.flowArtistPojos()
    val playlists = playlistDao.flowPlaylists()
    val tempAlbumPojos = _tempAlbumPojos.asStateFlow()
    val trackPojoPager: Pager<Int, TrackPojo> =
        Pager(config = PagingConfig(pageSize = 100)) { trackDao.pageTrackPojos() }

    fun addOrUpdateTempAlbum(pojo: AlbumWithTracksPojo) {
        _tempAlbumPojos.value += pojo.album.albumId to pojo
    }

    suspend fun addSelectionToPlaylist(selection: Selection, playlist: AbstractPlaylist) = database.withTransaction {
        val playlistTracks = getPlaylistTracksFromSelection(playlist, selection)
        playlistDao.insertPlaylistTracks(*playlistTracks.toTypedArray())
    }

    suspend fun deleteAlbums(albums: List<Album>) = albumDao.deleteAlbums(*albums.toTypedArray())

    suspend fun deleteAlbumWithTracks(pojo: AlbumWithTracksPojo) = database.withTransaction {
        trackDao.deleteTracks(*pojo.tracks.toTypedArray())
        albumDao.deleteAlbums(pojo.album)
    }

    suspend fun deleteAll() = database.withTransaction {
        trackDao.clearTracks()
        albumDao.clearAlbums()
    }

    suspend fun deleteTracks(tracks: List<Track>) = trackDao.deleteTracks(*tracks.toTypedArray())

    fun flowAlbumWithTracks(albumId: UUID) = albumDao.flowAlbumWithTracks(albumId)

    suspend fun getAlbumWithTracks(albumId: UUID) = albumDao.getAlbumWithTracks(albumId)

    suspend fun getImageBitmap(image: Image): ImageBitmap? {
        return _imageCacheMutex.withLock {
            if (!_imageCache.containsKey(image)) {
                _imageCache += image to image.getImageBitmap()
            }
            _imageCache[image]
        }
    }

    suspend fun insertPlaylist(playlist: Playlist, selection: Selection? = null) = database.withTransaction {
        val playlistTracks =
            selection?.let { getPlaylistTracksFromSelection(playlist, it) } ?: emptyList()
        playlistDao.upsertPlaylistWithTracks(playlist, playlistTracks)
    }

    suspend fun insertTrack(track: Track) = trackDao.insertTracks(track)

    suspend fun listAlbums(): List<Album> = albumDao.listAlbums()

    suspend fun listTracks(): List<Track> = trackDao.listTracks()

    fun pageTracksByArtist(artist: String): Pager<Int, TrackPojo> =
        Pager(config = PagingConfig(pageSize = 100)) { trackDao.pageTracksByArtist(artist) }

    fun pageTracksByPlaylistId(playlistId: UUID): Pager<Int, TrackPojo> =
        Pager(config = PagingConfig(pageSize = 100)) { trackDao.pageTracksByPlaylistId(playlistId) }

    suspend fun saveAlbumWithTracks(pojo: AlbumWithTracksPojo) = database.withTransaction {
        if (albumDao.albumExists(pojo.album.albumId)) {
            albumDao.updateAlbums(pojo.album)
            trackDao.deleteTracksByAlbumId(pojo.album.albumId)
            albumDao.clearAlbumGenres(pojo.album.albumId)
            albumDao.clearAlbumStyles(pojo.album.albumId)
        } else albumDao.insertAlbums(pojo.album)
        if (pojo.tracks.isNotEmpty())
            trackDao.insertTracks(*pojo.tracks.map { it.copy(albumId = pojo.album.albumId) }.toTypedArray())
        if (pojo.genres.isNotEmpty()) albumDao.insertAlbumGenres(pojo)
        if (pojo.styles.isNotEmpty()) albumDao.insertAlbumStyles(pojo)
    }

    suspend fun searchAlbums(query: String): List<AlbumPojo> = albumDao.simpleAlbumSearch(query)

    fun searchTracks(query: String): Pager<Int, TrackPojo> =
        Pager(config = PagingConfig(pageSize = 100)) { trackDao.simpleTrackSearch(query) }

    private suspend fun getPlaylistTracksFromSelection(
        playlist: AbstractPlaylist,
        selection: Selection,
    ): List<PlaylistTrack> {
        val playlistTracks = mutableListOf<PlaylistTrack>()
        var index = 0

        playlistTracks.addAll(
            selection.tracks.map { track ->
                PlaylistTrack(playlistId = playlist.playlistId, trackId = track.trackId, position = index++)
            }
        )
        playlistTracks.addAll(
            selection.queueTracks.map { queueTrack ->
                PlaylistTrack(
                    playlistId = playlist.playlistId,
                    trackId = queueTrack.trackId,
                    position = index++,
                )
            }
        )
        selection.albums.forEach { album ->
            albumDao.getAlbumWithTracks(album.albumId)?.tracks?.also { tracks ->
                playlistTracks.addAll(
                    tracks.map { track ->
                        PlaylistTrack(
                            playlistId = playlist.playlistId,
                            trackId = track.trackId,
                            position = index++,
                        )
                    }
                )
            }
        }
        return playlistTracks
    }
}
