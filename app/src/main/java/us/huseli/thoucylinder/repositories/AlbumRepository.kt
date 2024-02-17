package us.huseli.thoucylinder.repositories

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import us.huseli.thoucylinder.AlbumSortParameter
import us.huseli.thoucylinder.MutexCache
import us.huseli.thoucylinder.SortOrder
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.combos.AlbumCombo
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.combos.TrackCombo
import us.huseli.thoucylinder.dataclasses.combos.sortTags
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Tag
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumRepository @Inject constructor(database: Database, @ApplicationContext context: Context) {
    private val albumDao = database.albumDao()
    private val selectedAlbums = mutableMapOf<String, MutableStateFlow<List<Album>>>()

    val thumbnailCache = MutexCache<MediaStoreImage, ImageBitmap> {
        it.getThumbnailImageBitmap(context)
    }

    suspend fun addToLibrary(albumId: UUID) = albumDao.setIsInLibrary(listOf(albumId), true)

    suspend fun deleteAlbums(albums: Collection<Album>) = albumDao.deleteAlbums(*albums.toTypedArray())

    suspend fun deleteTempAlbums() = albumDao.deleteTempAlbums()

    fun flowAlbumCombos(
        sortParameter: AlbumSortParameter,
        sortOrder: SortOrder,
        searchTerm: String,
    ): Flow<List<AlbumCombo>> =
        albumDao.flowAlbumCombos(sortParameter = sortParameter, sortOrder = sortOrder, searchTerm = searchTerm)
            .map { it.sortTags() }

    fun flowAlbumCombosByArtist(artist: String) = albumDao.flowAlbumCombosByArtist(artist)

    fun flowAlbumWithTracks(albumId: UUID): Flow<AlbumWithTracksCombo?> =
        albumDao.flowAlbumWithTracks(albumId).map { it?.sorted() }

    fun flowTags(): Flow<List<Tag>> = albumDao.flowTags()

    fun flowSelectedAlbums(viewModelClass: String): StateFlow<List<Album>> =
        mutableFlowSelectedAlbums(viewModelClass).asStateFlow()

    suspend fun getAlbum(albumId: UUID): Album? = albumDao.getAlbum(albumId)

    suspend fun getAlbumWithTracks(albumId: UUID): AlbumWithTracksCombo? =
        albumDao.getAlbumWithTracks(albumId)?.sorted()

    suspend fun insertAlbumCombo(combo: AbstractAlbumCombo) = albumDao.insertAlbumCombos(listOf(combo))

    suspend fun insertAlbumCombos(combos: Collection<AbstractAlbumCombo>) = albumDao.insertAlbumCombos(combos)

    suspend fun insertTags(tags: Collection<Tag>) = albumDao.insertTags(*tags.toTypedArray())

    suspend fun listAlbums(): List<Album> = albumDao.listAlbums()

    suspend fun listAlbumTrackCombos(albumIds: List<UUID>): List<TrackCombo> = albumDao.listTrackCombos(albumIds)

    suspend fun listDeletionMarkedAlbums(): List<Album> = albumDao.listDeletionMarkedAlbums()

    suspend fun listTags(): List<Tag> = albumDao.listTags()

    fun selectAlbums(selectionKey: String, albums: Iterable<Album>) {
        mutableFlowSelectedAlbums(selectionKey).also { flow ->
            val currentIds = flow.value.map { album -> album.albumId }
            flow.value += albums.filter { album -> !currentIds.contains(album.albumId) }
        }
    }

    suspend fun setAlbumIsDeleted(albumId: UUID, isDeleted: Boolean) = albumDao.setIsDeleted(albumId, isDeleted)

    suspend fun setAlbumIsHidden(albumId: UUID, value: Boolean) = albumDao.setIsHidden(albumId, value)

    suspend fun setAlbumIsInLibrary(albumId: UUID, value: Boolean) = albumDao.setIsInLibrary(listOf(albumId), value)

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
        if (albumArt != null) albumDao.updateAlbumArt(albumId, albumArt)
        else albumDao.clearAlbumArt(albumId)
    }

    suspend fun updateAlbumCombo(combo: AbstractAlbumCombo) = albumDao.updateAlbumCombo(combo)

    private fun mutableFlowSelectedAlbums(viewModelClass: String): MutableStateFlow<List<Album>> =
        selectedAlbums[viewModelClass] ?: MutableStateFlow<List<Album>>(emptyList()).also {
            selectedAlbums[viewModelClass] = it
        }
}
