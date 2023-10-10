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
import us.huseli.thoucylinder.dataclasses.AlbumPojo
import us.huseli.thoucylinder.dataclasses.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.Image
import us.huseli.thoucylinder.dataclasses.entities.AbstractPlaylist
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
    val trackPager: Pager<Int, Track> = Pager(config = PagingConfig(pageSize = 100)) { trackDao.pageTracks() }

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

    fun getAlbumWithTracks(albumId: UUID) = albumDao.flowAlbumWithTracks(albumId)

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

    fun pageTracksByArtist(artist: String): Pager<Int, Track> =
        Pager(config = PagingConfig(pageSize = 100)) { trackDao.pageTracksByArtist(artist) }

    fun pageTracksByPlaylistId(playlistId: UUID): Pager<Int, Track> =
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

    fun searchTracks(query: String): Pager<Int, Track> =
        Pager(config = PagingConfig(pageSize = 100)) { trackDao.simpleTrackSearch(query) }

    private suspend fun getPlaylistTracksFromSelection(
        playlist: AbstractPlaylist,
        selection: Selection,
    ): List<PlaylistTrack> {
        // TODO: This is a little stupid. If, potentially, tracks _and_ albums were selected at the same time,
        // PlaylistTracks would get identical values for "position".
        val playlistTracks = mutableListOf<PlaylistTrack>()

        playlistTracks.addAll(
            selection.tracks.mapIndexed { index, track ->
                PlaylistTrack(playlistId = playlist.playlistId, trackId = track.trackId, position = index)
            }
        )
        playlistTracks.addAll(
            selection.queueTracks.mapIndexed { index, queueTrack ->
                PlaylistTrack(playlistId = playlist.playlistId, trackId = queueTrack.trackId, position = index)
            }
        )
        selection.albums.forEachIndexed { albumIdx, album ->
            albumDao.getAlbumWithTracks(album.albumId)?.tracks?.also { tracks ->
                playlistTracks.addAll(
                    tracks.mapIndexed { trackIdx, track ->
                        PlaylistTrack(
                            playlistId = playlist.playlistId,
                            trackId = track.trackId,
                            position = albumIdx + trackIdx,
                        )
                    }
                )
            }
        }
        return playlistTracks
    }
}
