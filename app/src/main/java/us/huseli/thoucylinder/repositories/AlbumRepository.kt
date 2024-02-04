package us.huseli.thoucylinder.repositories

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import us.huseli.thoucylinder.AlbumSortParameter
import us.huseli.thoucylinder.SortOrder
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.dataclasses.pojos.AlbumPojo
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import us.huseli.thoucylinder.dataclasses.pojos.sortGenres
import us.huseli.thoucylinder.dataclasses.pojos.sortStyles
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumRepository @Inject constructor(private val database: Database) {
    private val albumDao = database.albumDao()
    private val _selectedAlbums = mutableMapOf<String, MutableStateFlow<List<Album>>>()

    suspend fun addToLibrary(albumId: UUID) = albumDao.setAlbumIsInLibrary(albumId, true)

    suspend fun deleteAlbums(albums: Collection<Album>) =
        albumDao.deleteAlbums(*albums.toTypedArray())

    suspend fun deleteAlbumPojos(pojos: Iterable<AbstractAlbumPojo>) =
        albumDao.deleteAlbums(*pojos.map { it.album }.toTypedArray())

    suspend fun deleteTempAlbums() = albumDao.deleteTempAlbums()

    fun flowAlbumPojos(
        sortParameter: AlbumSortParameter,
        sortOrder: SortOrder,
        searchTerm: String,
    ): Flow<List<AlbumPojo>> =
        albumDao.flowAlbumPojos(sortParameter = sortParameter, sortOrder = sortOrder, searchTerm = searchTerm)
            .map { it.sortGenres().sortStyles() }

    fun flowAlbumPojosByArtist(artist: String) = albumDao.flowAlbumPojosByArtist(artist)

    fun flowAlbumWithTracks(albumId: UUID): Flow<AlbumWithTracksPojo?> =
        albumDao.flowAlbumWithTracks(albumId).map { it?.sorted() }

    fun flowGenres(): Flow<List<Genre>> = albumDao.flowGenres()

    fun flowSelectedAlbums(viewModelClass: String): StateFlow<List<Album>> =
        mutableFlowSelectedAlbums(viewModelClass).asStateFlow()

    suspend fun getAlbum(albumId: UUID): Album? = albumDao.getAlbum(albumId)

    suspend fun getAlbumWithTracks(albumId: UUID): AlbumWithTracksPojo? =
        albumDao.getAlbumWithTracks(albumId)?.sorted()

    suspend fun insertAlbums(albums: Collection<Album>) {
        if (albums.isNotEmpty()) albumDao.insertAlbums(*albums.toTypedArray())
    }

    suspend fun insertGenres(genres: Collection<Genre>) = albumDao.insertGenres(*genres.toTypedArray())

    suspend fun listAlbums(): List<Album> = albumDao.listAlbums()

    suspend fun listAlbumTrackPojos(albumIds: List<UUID>): List<TrackPojo> = albumDao.listTrackPojos(albumIds)

    suspend fun listDeletionMarkedAlbums(): List<Album> = albumDao.listDeletionMarkedAlbums()

    suspend fun listGenres(): List<Genre> = albumDao.listGenres()

    suspend fun saveAlbum(album: Album) = database.withTransaction {
        if (albumDao.albumExists(album.albumId)) albumDao.updateAlbums(album)
        else albumDao.insertAlbums(album)
    }

    suspend fun saveAlbumGenres(albumId: UUID, genres: Collection<Genre>) = database.withTransaction {
        if (albumDao.albumExists(albumId)) albumDao.clearAlbumGenres(albumId)
        if (genres.isNotEmpty()) albumDao.insertAlbumGenres(albumId, genres)
    }

    suspend fun saveAlbumPojo(pojo: AbstractAlbumPojo) = database.withTransaction {
        if (albumDao.albumExists(pojo.album.albumId)) {
            albumDao.updateAlbums(pojo.album)
            albumDao.clearAlbumGenres(pojo.album.albumId)
            albumDao.clearAlbumStyles(pojo.album.albumId)
        } else albumDao.insertAlbums(pojo.album)
        if (pojo.genres.isNotEmpty()) albumDao.insertAlbumGenres(pojo.album.albumId, pojo.genres)
        if (pojo.styles.isNotEmpty()) albumDao.insertAlbumStyles(pojo.album.albumId, pojo.styles)
    }

    fun selectAlbums(selectionKey: String, albums: Iterable<Album>) {
        mutableFlowSelectedAlbums(selectionKey).also { flow ->
            val currentIds = flow.value.map { album -> album.albumId }
            flow.value += albums.filter { album -> !currentIds.contains(album.albumId) }
        }
    }

    suspend fun setAlbumIsDeleted(albumId: UUID, isDeleted: Boolean) = albumDao.setAlbumIsDeleted(albumId, isDeleted)

    suspend fun setAlbumIsHidden(albumId: UUID, value: Boolean) = albumDao.setAlbumIsHidden(albumId, value)

    suspend fun setAlbumIsLocal(albumId: UUID, isLocal: Boolean) = albumDao.setAlbumIsLocal(albumId, isLocal)

    fun toggleAlbumSelected(selectionKey: String, album: Album) {
        mutableFlowSelectedAlbums(selectionKey).also {
            if (it.value.contains(album)) it.value -= album
            else it.value += album
        }
    }

    fun unselectAllAlbums(selectionKey: String) {
        mutableFlowSelectedAlbums(selectionKey).value = emptyList()
    }

    suspend fun updateAlbums(albums: Collection<Album>) = albumDao.updateAlbums(*albums.toTypedArray())

    private fun mutableFlowSelectedAlbums(viewModelClass: String): MutableStateFlow<List<Album>> =
        _selectedAlbums[viewModelClass] ?: MutableStateFlow<List<Album>>(emptyList()).also {
            _selectedAlbums[viewModelClass] = it
        }
}
