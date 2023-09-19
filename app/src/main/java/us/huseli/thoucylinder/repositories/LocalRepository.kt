package us.huseli.thoucylinder.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import us.huseli.thoucylinder.data.dao.AlbumDao
import us.huseli.thoucylinder.data.dao.TrackDao
import us.huseli.thoucylinder.data.entities.Album
import us.huseli.thoucylinder.data.entities.Track
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalRepository @Inject constructor(
    private val albumDao: AlbumDao,
    private val trackDao: TrackDao,
) {
    val albums: Flow<List<Album>> = albumDao.listWithTracks().map { albumMap ->
        albumMap.map { (album, tracks) ->
            album.copy(tracks = tracks)
        }
    }
    val singleTracks: Flow<List<Track>> = trackDao.listSingle()
    val tracks: Flow<List<Track>> = trackDao.list()

    suspend fun insertAlbumWithTracks(album: Album) = albumDao.insertWithTracks(album)

    suspend fun insertTrack(track: Track) = trackDao.insert(track)
}
