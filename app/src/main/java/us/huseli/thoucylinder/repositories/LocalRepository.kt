package us.huseli.thoucylinder.repositories

import androidx.compose.ui.graphics.ImageBitmap
import androidx.paging.Pager
import androidx.paging.PagingConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.database.MusicDao
import us.huseli.thoucylinder.dataclasses.AbstractPlaylist
import us.huseli.thoucylinder.dataclasses.Album
import us.huseli.thoucylinder.dataclasses.AlbumPojo
import us.huseli.thoucylinder.dataclasses.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.Image
import us.huseli.thoucylinder.dataclasses.Playlist
import us.huseli.thoucylinder.dataclasses.Track
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class LocalRepository @Inject constructor(private val musicDao: MusicDao) {
    private val _tempAlbumPojos = MutableStateFlow<Map<UUID, AlbumWithTracksPojo>>(emptyMap())
    private val _imageCache = mutableMapOf<Image, ImageBitmap?>()
    private val _imageCacheMutex = Mutex()

    val albumPojos: Flow<List<AlbumPojo>> = musicDao.flowAlbumPojos()
    val artistPojos = musicDao.flowArtistPojos()
    val playlists = musicDao.flowPlaylists()
    val tempAlbumPojos = _tempAlbumPojos.asStateFlow()
    val trackPager = Pager(config = PagingConfig(pageSize = 100)) { musicDao.pageTracks() }

    fun addOrUpdateTempAlbum(pojo: AlbumWithTracksPojo) {
        _tempAlbumPojos.value += pojo.album.albumId to pojo
    }

    suspend fun addSelectionToPlaylist(selection: Selection, playlist: AbstractPlaylist) =
        musicDao.addSelectionToPlaylist(selection, playlist)

    suspend fun deleteAlbums(albums: List<Album>) = musicDao.deleteAlbums(*albums.toTypedArray())

    suspend fun deleteAlbumWithTracks(album: AlbumWithTracksPojo) = musicDao.deleteAlbumWithTracks(album)

    suspend fun deleteAll() = musicDao.deleteAll()

    suspend fun deleteTracks(tracks: List<Track>) = musicDao.deleteTracks(*tracks.toTypedArray())

    fun getAlbumWithTracks(albumId: UUID) = musicDao.flowAlbumWithTracks(albumId)

    suspend fun getImageBitmap(image: Image): ImageBitmap? {
        return _imageCacheMutex.withLock {
            if (!_imageCache.containsKey(image)) {
                _imageCache += image to image.getImageBitmap()
            }
            _imageCache[image]
        }
    }

    suspend fun insertPlaylist(playlist: Playlist, trackIds: List<UUID>) =
        musicDao.upsertPlaylistWithTracks(playlist, trackIds)

    suspend fun insertTrack(track: Track) = musicDao.insertTrack(track = track)

    suspend fun listAlbums(): List<Album> = musicDao.listAlbums()

    suspend fun listTracks(): List<Track> = musicDao.listTracks()

    fun pageTracksByArtist(artist: String): Pager<Int, Track> =
        Pager(config = PagingConfig(pageSize = 100)) { musicDao.pageTracksByArtist(artist) }

    fun pageTracksByPlaylistId(playlistId: UUID): Pager<Int, Track> =
        Pager(config = PagingConfig(pageSize = 100)) { musicDao.pageTracksByPlaylistId(playlistId) }

    suspend fun saveAlbumWithTracks(pojo: AlbumWithTracksPojo) = musicDao.upsertAlbumWithTracks(pojo)

    suspend fun updateTracks(vararg tracks: Track) = musicDao.updateTracks(*tracks)
}
