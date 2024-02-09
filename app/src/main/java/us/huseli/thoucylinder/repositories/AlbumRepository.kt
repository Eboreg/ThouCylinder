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
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.AlbumGenre
import us.huseli.thoucylinder.dataclasses.entities.AlbumStyle
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.dataclasses.combos.AlbumCombo
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.combos.TrackCombo
import us.huseli.thoucylinder.dataclasses.combos.sortGenres
import us.huseli.thoucylinder.dataclasses.combos.sortStyles
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumRepository @Inject constructor(private val database: Database) {
    private val albumDao = database.albumDao()
    private val _selectedAlbums = mutableMapOf<String, MutableStateFlow<List<Album>>>()

    suspend fun addToLibrary(albumId: UUID) = albumDao.setIsInLibrary(listOf(albumId), true)

    suspend fun deleteAlbums(albums: Collection<Album>) = albumDao.deleteAlbums(*albums.toTypedArray())

    suspend fun deleteTempAlbums() = albumDao.deleteTempAlbums()

    fun flowAlbumCombos(
        sortParameter: AlbumSortParameter,
        sortOrder: SortOrder,
        searchTerm: String,
    ): Flow<List<AlbumCombo>> =
        albumDao.flowAlbumCombos(sortParameter = sortParameter, sortOrder = sortOrder, searchTerm = searchTerm)
            .map { it.sortGenres().sortStyles() }

    fun flowAlbumCombosByArtist(artist: String) = albumDao.flowAlbumCombosByArtist(artist)

    fun flowAlbumWithTracks(albumId: UUID): Flow<AlbumWithTracksCombo?> =
        albumDao.flowAlbumWithTracks(albumId).map { it?.sorted() }

    fun flowGenres(): Flow<List<Genre>> = albumDao.flowGenres()

    fun flowSelectedAlbums(viewModelClass: String): StateFlow<List<Album>> =
        mutableFlowSelectedAlbums(viewModelClass).asStateFlow()

    suspend fun getAlbum(albumId: UUID): Album? = albumDao.getAlbum(albumId)

    suspend fun getAlbumWithTracks(albumId: UUID): AlbumWithTracksCombo? =
        albumDao.getAlbumWithTracks(albumId)?.sorted()

    suspend fun insertAlbums(albums: Collection<Album>) {
        if (albums.isNotEmpty()) albumDao.insertAlbums(*albums.toTypedArray())
    }

    suspend fun insertGenres(genres: Collection<Genre>) = albumDao.insertGenres(*genres.toTypedArray())

    suspend fun listAlbums(): List<Album> = albumDao.listAlbums()

    suspend fun listAlbumTrackCombos(albumIds: List<UUID>): List<TrackCombo> = albumDao.listTrackCombos(albumIds)

    suspend fun listDeletionMarkedAlbums(): List<Album> = albumDao.listDeletionMarkedAlbums()

    suspend fun listGenres(): List<Genre> = albumDao.listGenres()

    suspend fun saveAlbumGenres(albumId: UUID, genres: Collection<Genre>) = database.withTransaction {
        if (albumDao.albumExists(albumId)) albumDao.clearAlbumGenres(listOf(albumId))
        if (genres.isNotEmpty()) albumDao.insertAlbumGenres(genres.map { AlbumGenre(albumId, it.genreName) })
    }

    suspend fun saveAlbumCombos(combos: Iterable<AbstractAlbumCombo>) = database.withTransaction {
        val existingIds = albumDao.listExistingAlbumIds(combos.map { it.album.albumId })
        val existing = combos.filter { existingIds.contains(it.album.albumId) }
        val nonExisting = combos.minus(existing.toSet())
        val albumGenres = combos.flatMap { combo -> combo.genres.map { AlbumGenre(combo.album.albumId, it.genreName) } }
        val albumStyles = combos.flatMap { combo -> combo.styles.map { AlbumStyle(combo.album.albumId, it.styleName) } }

        if (existing.isNotEmpty()) {
            albumDao.updateAlbums(*existing.map { it.album }.toTypedArray())
            albumDao.clearAlbumGenres(existingIds)
            albumDao.clearAlbumStyles(existingIds)
        }
        if (nonExisting.isNotEmpty()) albumDao.insertAlbums(*nonExisting.map { it.album }.toTypedArray())
        if (albumGenres.isNotEmpty()) albumDao.insertAlbumGenres(albumGenres)
        if (albumStyles.isNotEmpty()) albumDao.insertAlbumStyles(albumStyles)
    }

    suspend fun saveAlbumCombo(combo: AbstractAlbumCombo) = saveAlbumCombos(listOf(combo))

    fun selectAlbums(selectionKey: String, albums: Iterable<Album>) {
        mutableFlowSelectedAlbums(selectionKey).also { flow ->
            val currentIds = flow.value.map { album -> album.albumId }
            flow.value += albums.filter { album -> !currentIds.contains(album.albumId) }
        }
    }

    suspend fun setAlbumIsDeleted(albumId: UUID, isDeleted: Boolean) = albumDao.setIsDeleted(albumId, isDeleted)

    suspend fun setAlbumIsHidden(albumId: UUID, value: Boolean) = albumDao.setIsHidden(albumId, value)

    suspend fun setAlbumIsLocal(albumId: UUID, isLocal: Boolean) = albumDao.setIsLocal(listOf(albumId), isLocal)

    suspend fun setAlbumsIsLocal(albumIds: Collection<UUID>, isLocal: Boolean) = albumDao.setIsLocal(albumIds, isLocal)

    fun toggleAlbumSelected(selectionKey: String, album: Album) {
        mutableFlowSelectedAlbums(selectionKey).also {
            if (it.value.contains(album)) it.value -= album
            else it.value += album
        }
    }

    fun unselectAllAlbums(selectionKey: String) {
        mutableFlowSelectedAlbums(selectionKey).value = emptyList()
    }

    suspend fun updateAlbum(album: Album) = albumDao.updateAlbums(album)

    suspend fun updateAlbumArt(albumId: UUID, albumArt: MediaStoreImage?) {
        if (albumArt != null) albumDao.updateAlbumArt(albumId, albumArt.uri, albumArt.thumbnailUri)
        else albumDao.clearAlbumArt(albumId)
    }

    private fun mutableFlowSelectedAlbums(viewModelClass: String): MutableStateFlow<List<Album>> =
        _selectedAlbums[viewModelClass] ?: MutableStateFlow<List<Album>>(emptyList()).also {
            _selectedAlbums[viewModelClass] = it
        }
}
