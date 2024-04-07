package us.huseli.thoucylinder.repositories

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Tag
import us.huseli.thoucylinder.dataclasses.views.AlbumCombo
import us.huseli.thoucylinder.enums.AlbumSortParameter
import us.huseli.thoucylinder.enums.AvailabilityFilter
import us.huseli.thoucylinder.enums.SortOrder
import us.huseli.thoucylinder.getCachedFullBitmap
import us.huseli.thoucylinder.getCachedThumbnailBitmap
import us.huseli.thoucylinder.getMutexCache
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumRepository @Inject constructor(database: Database, @ApplicationContext private val context: Context) {
    private val albumDao = database.albumDao()
    private val selectedAlbumIds = mutableMapOf<String, MutableStateFlow<List<String>>>()

    val thumbnailCache = getMutexCache<Uri, ImageBitmap> { uri ->
        uri.getCachedThumbnailBitmap(context)?.asImageBitmap()
    }

    suspend fun addAlbumsToLibrary(albumIds: Collection<String>) =
        albumDao.setIsInLibrary(true, *albumIds.toTypedArray())

    suspend fun clearAlbums() = albumDao.clearAlbums()

    suspend fun clearTags() = albumDao.clearTags()

    suspend fun deleteAlbums(albums: Collection<Album>) {
        if (albums.isNotEmpty()) albumDao.deleteAlbums(*albums.toTypedArray())
    }

    suspend fun deleteTempAlbums() = albumDao.deleteTempAlbums()

    fun flowAlbumCombos(
        sortParameter: AlbumSortParameter,
        sortOrder: SortOrder,
        searchTerm: String,
        tagNames: Collection<String>,
        availabilityFilter: AvailabilityFilter,
    ): Flow<List<AlbumCombo>> = albumDao.flowAlbumCombos(
        sortParameter = sortParameter,
        sortOrder = sortOrder,
        searchTerm = searchTerm,
        tagNames = tagNames,
        availabilityFilter = availabilityFilter,
    ).distinctUntilChanged()

    fun flowAlbumCombosByArtist(artistId: String): Flow<List<AlbumCombo>> = albumDao.flowAlbumCombosByArtist(artistId)

    fun flowAlbumWithTracks(albumId: String): Flow<AlbumWithTracksCombo?> = albumDao.flowAlbumWithTracks(albumId)

    fun flowSelectedAlbumIds(viewModelClass: String): StateFlow<List<String>> =
        mutableFlowSelectedAlbumIds(viewModelClass).asStateFlow()

    fun flowTagPojos(availabilityFilter: AvailabilityFilter) = albumDao.flowTagPojos(availabilityFilter)

    fun flowTags(): Flow<List<Tag>> = albumDao.flowTags()

    suspend fun getAlbumCombo(albumId: String): AlbumCombo? = albumDao.getAlbumCombo(albumId)

    suspend fun getAlbumWithTracks(albumId: String): AlbumWithTracksCombo? = albumDao.getAlbumWithTracks(albumId)

    suspend fun getAlbumWithTracksByPlaylistId(playlistId: String) = albumDao.getAlbumWithTracksByPlaylistId(playlistId)

    suspend fun getFullImage(uri: Uri?): ImageBitmap? = uri?.getCachedFullBitmap(context)?.asImageBitmap()

    suspend fun getFullImage(album: Album): ImageBitmap? =
        album.albumArt?.fullUri?.getCachedThumbnailBitmap(context)?.asImageBitmap()

    suspend fun insertTags(tags: Collection<Tag>) {
        if (tags.isNotEmpty()) albumDao.insertTags(*tags.toTypedArray())
    }

    suspend fun listAlbumCombos(): List<AlbumCombo> = albumDao.listAlbumCombos()

    suspend fun listAlbums(): List<Album> = albumDao.listAlbums()

    suspend fun listAlbumsWithTracks(albumIds: Collection<String>): List<AlbumWithTracksCombo> =
        if (albumIds.isNotEmpty()) albumDao.listAlbumsWithTracks(*albumIds.toTypedArray()) else emptyList()

    suspend fun listDeletionMarkedAlbumCombos(): List<AlbumCombo> = albumDao.listDeletionMarkedAlbumCombos()

    suspend fun listTags(): List<Tag> = albumDao.listTags()

    suspend fun removeAlbumsFromLibrary(albumIds: Collection<String>) =
        albumDao.setIsInLibrary(false, *albumIds.toTypedArray())

    fun selectAlbumIds(selectionKey: String, albumIds: Iterable<String>) {
        mutableFlowSelectedAlbumIds(selectionKey).also { flow ->
            val currentIds = flow.value
            flow.value += albumIds.filter { albumId -> !currentIds.contains(albumId) }
        }
    }

    suspend fun setAlbumsIsHidden(albumIds: Collection<String>, value: Boolean) =
        albumDao.setIsHidden(value, *albumIds.toTypedArray())

    suspend fun setAlbumsIsLocal(albumIds: Collection<String>, isLocal: Boolean) {
        if (albumIds.isNotEmpty()) albumDao.setIsLocal(isLocal, *albumIds.toTypedArray())
    }

    suspend fun setAlbumTags(albumId: String, tags: Collection<Tag>) = albumDao.setAlbumTags(albumId, tags)

    fun toggleAlbumIdSelected(selectionKey: String, albumId: String) {
        mutableFlowSelectedAlbumIds(selectionKey).also {
            if (it.value.contains(albumId)) it.value -= albumId
            else it.value += albumId
        }
    }

    fun unselectAllAlbumIds(selectionKey: String) {
        mutableFlowSelectedAlbumIds(selectionKey).value = emptyList()
    }

    suspend fun updateAlbum(album: Album) = albumDao.updateAlbums(album)

    suspend fun updateAlbumArt(albumId: String, albumArt: MediaStoreImage?) {
        if (albumArt != null) albumDao.updateAlbumArt(albumId, albumArt)
        else albumDao.clearAlbumArt(albumId)
    }

    suspend fun upsertAlbumAndTags(combo: AlbumWithTracksCombo) = albumDao.upsertAlbumsAndTags(listOf(combo))

    suspend fun upsertAlbumsAndTags(combos: Collection<AlbumWithTracksCombo>) = albumDao.upsertAlbumsAndTags(combos)

    private fun mutableFlowSelectedAlbumIds(viewModelClass: String): MutableStateFlow<List<String>> =
        selectedAlbumIds[viewModelClass] ?: MutableStateFlow<List<String>>(emptyList()).also {
            selectedAlbumIds[viewModelClass] = it
        }
}
