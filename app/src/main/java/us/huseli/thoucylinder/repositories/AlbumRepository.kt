package us.huseli.thoucylinder.repositories

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
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
class AlbumRepository @Inject constructor(
    private val database: Database,
    @ApplicationContext private val context: Context,
) {
    private val albumDao = database.albumDao()

    private val _selectedAlbums = mutableMapOf<String, MutableStateFlow<List<Album>>>()

    fun deleteAlbumArt(album: Album) = album.albumArt?.delete(context)

    suspend fun deleteAlbum(album: Album) = deleteAlbums(listOf(album))

    suspend fun deleteAlbums(albums: Collection<Album>) = albumDao.deleteAlbums(*albums.toTypedArray())

    @WorkerThread
    fun deleteAlbumLocalImageFiles(pojo: AlbumWithTracksPojo) {
        pojo.listCoverImages(context, includeThumbnails = true).forEach { documentFile ->
            if (documentFile.isFile && documentFile.canWrite()) documentFile.delete()
        }
    }

    suspend fun deleteMarkedAlbums() = database.withTransaction {
        albumDao.deleteTracksFromMarkedAlbums()
        albumDao.deleteMarkedAlbums()
    }

    suspend fun deleteTempAlbums() = albumDao.deleteTempAlbums()

    fun flowAlbumPojos(sortParameter: AlbumSortParameter, sortOrder: SortOrder, searchTerm: String): Flow<List<AlbumPojo>> =
        albumDao.flowAlbumPojos(sortParameter = sortParameter, sortOrder = sortOrder, searchTerm = searchTerm)
            .map { it.sortGenres().sortStyles() }

    fun flowAlbumPojosByArtist(artist: String) = albumDao.flowAlbumPojosByArtist(artist)

    fun flowAlbumWithTracks(albumId: UUID): Flow<AlbumWithTracksPojo?> =
        albumDao.flowAlbumWithTracks(albumId).map { it?.sorted() }

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

    suspend fun listGenres(): List<Genre> = albumDao.listGenres()

    suspend fun saveAlbumPojo(pojo: AbstractAlbumPojo) = database.withTransaction {
        if (albumDao.albumExists(pojo.album.albumId)) {
            albumDao.updateAlbums(pojo.album)
            albumDao.clearAlbumGenres(pojo.album.albumId)
            albumDao.clearAlbumStyles(pojo.album.albumId)
        } else albumDao.insertAlbums(pojo.album)
        if (pojo.genres.isNotEmpty()) albumDao.insertAlbumGenres(pojo)
        if (pojo.styles.isNotEmpty()) albumDao.insertAlbumStyles(pojo)
    }

    fun selectAlbums(selectionKey: String, albums: Iterable<Album>) {
        mutableFlowSelectedAlbums(selectionKey).also { flow ->
            val currentIds = flow.value.map { album -> album.albumId }
            flow.value += albums.filter { album -> !currentIds.contains(album.albumId) }
        }
    }

    fun toggleAlbumSelected(selectionKey: String, album: Album) {
        mutableFlowSelectedAlbums(selectionKey).also {
            if (it.value.contains(album)) it.value -= album
            else it.value += album
        }
    }

    fun unselectAllAlbums(selectionKey: String) {
        mutableFlowSelectedAlbums(selectionKey).value = emptyList()
    }

    suspend fun updateAlbum(album: Album) = updateAlbums(listOf(album))

    suspend fun updateAlbums(albums: Collection<Album>) = albumDao.updateAlbums(*albums.toTypedArray())

    private fun mutableFlowSelectedAlbums(viewModelClass: String): MutableStateFlow<List<Album>> =
        _selectedAlbums[viewModelClass] ?: MutableStateFlow<List<Album>>(emptyList()).also {
            _selectedAlbums[viewModelClass] = it
        }
}
