package us.huseli.thoucylinder.repositories

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import us.huseli.thoucylinder.AbstractScopeHolder
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.album.Album
import us.huseli.thoucylinder.dataclasses.album.AlbumCombo
import us.huseli.thoucylinder.dataclasses.album.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.album.IAlbum
import us.huseli.thoucylinder.dataclasses.album.IAlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.tag.Tag
import us.huseli.thoucylinder.enums.AlbumSortParameter
import us.huseli.thoucylinder.enums.AvailabilityFilter
import us.huseli.thoucylinder.enums.SortOrder
import us.huseli.thoucylinder.imageCacheDir
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumRepository @Inject constructor(
    database: Database,
    @ApplicationContext private val context: Context,
) : AbstractScopeHolder() {
    private val albumDao = database.albumDao()
    private val selectedAlbumIds = mutableMapOf<String, MutableStateFlow<List<String>>>()

    init {
        launchOnIOThread { cleanImageCache() }
    }

    suspend fun addAlbumsToLibrary(albumIds: Collection<String>) =
        onIOThread { albumDao.addToLibrary(*albumIds.toTypedArray()) }

    suspend fun clearAlbumArt(albumId: String) = onIOThread { albumDao.clearAlbumArt(albumId) }

    suspend fun clearAlbums() = onIOThread { albumDao.clearAlbums() }

    suspend fun clearTags() = onIOThread { albumDao.clearTags() }

    suspend fun deleteTempAlbums() = onIOThread { albumDao.deleteTempAlbums() }

    fun flowAlbumCombo(albumId: String): Flow<AlbumCombo?> = albumDao.flowAlbumCombo(albumId)

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

    fun flowTagsByAlbumId(albumId: String): Flow<List<Tag>> = albumDao.flowTagsByAlbumId(albumId)

    suspend fun getAlbum(albumId: String): Album? = onIOThread { albumDao.getAlbum(albumId) }

    suspend fun getAlbumCombo(albumId: String): AlbumCombo? = onIOThread { albumDao.getAlbumCombo(albumId) }

    suspend fun getAlbumWithTracks(albumId: String): AlbumWithTracksCombo? =
        onIOThread { albumDao.getAlbumWithTracks(albumId) }

    suspend fun getAlbumWithTracksByPlaylistId(playlistId: String) =
        onIOThread { albumDao.getAlbumWithTracksByPlaylistId(playlistId) }

    suspend fun getOrCreateAlbumBySpotifyId(album: IAlbum, spotifyId: String): Album =
        onIOThread { albumDao.getOrCreateAlbumBySpotifyId(album, spotifyId) }

    suspend fun insertTags(tags: Collection<Tag>) {
        if (tags.isNotEmpty()) onIOThread { albumDao.insertTags(*tags.toTypedArray()) }
    }

    suspend fun listAlbumCombos(): List<AlbumCombo> = onIOThread { albumDao.listAlbumCombos() }

    suspend fun listAlbumCombos(albumIds: Collection<String>): List<AlbumCombo> =
        if (albumIds.isNotEmpty()) onIOThread { albumDao.listAlbumCombos(*albumIds.toTypedArray()) }
        else emptyList()

    suspend fun listAlbums(): List<Album> = onIOThread { albumDao.listAlbums() }

    suspend fun listAlbumsWithTracks(): List<AlbumWithTracksCombo> = onIOThread { albumDao.listAlbumsWithTracks() }

    suspend fun listAlbumsWithTracks(albumIds: Collection<String>): List<AlbumWithTracksCombo> =
        if (albumIds.isNotEmpty()) onIOThread { albumDao.listAlbumsWithTracks(*albumIds.toTypedArray()) }
        else emptyList()

    suspend fun listMusicBrainzReleaseGroupAlbumCombos(): Map<String, AlbumCombo> =
        albumDao.listMusicBrainzReleaseGroupAlbumCombos()

    suspend fun listSpotifyAlbumCombos(): Map<String, AlbumCombo> = albumDao.listSpotifyAlbumCombos()

    suspend fun listTags(): List<Tag> = onIOThread { albumDao.listTags() }

    suspend fun listTags(albumId: String): List<Tag> = onIOThread { albumDao.listTags(albumId) }

    suspend fun listYoutubeAlbumCombos(): Map<String, AlbumCombo> = albumDao.listYoutubeAlbumCombos()

    fun selectAlbumIds(selectionKey: String, albumIds: Iterable<String>) {
        mutableFlowSelectedAlbumIds(selectionKey).also { flow ->
            val currentIds = flow.value
            flow.value += albumIds.filter { albumId -> !currentIds.contains(albumId) }
        }
    }

    suspend fun setAlbumsIsHidden(albumIds: Collection<String>, value: Boolean) =
        onIOThread { albumDao.setIsHidden(value, *albumIds.toTypedArray()) }

    suspend fun setAlbumsIsLocal(albumIds: Collection<String>, isLocal: Boolean) {
        if (albumIds.isNotEmpty()) onIOThread { albumDao.setIsLocal(isLocal, *albumIds.toTypedArray()) }
    }

    suspend fun setAlbumTags(albumId: String, tags: Collection<Tag>) =
        onIOThread { albumDao.setAlbumTags(albumId, tags) }

    fun toggleAlbumIdSelected(selectionKey: String, albumId: String) {
        mutableFlowSelectedAlbumIds(selectionKey).also {
            if (it.value.contains(albumId)) it.value -= albumId
            else it.value += albumId
        }
    }

    suspend fun unhideLocalAlbums() = onIOThread { albumDao.unhideLocalAlbums() }

    fun unselectAllAlbumIds(selectionKey: String) {
        mutableFlowSelectedAlbumIds(selectionKey).value = emptyList()
    }

    suspend fun updateAlbumArt(albumId: String, albumArt: MediaStoreImage?) {
        onIOThread {
            if (albumArt != null) albumDao.updateAlbumArt(albumId, albumArt)
            else albumDao.clearAlbumArt(albumId)
        }
    }

    suspend fun upsertAlbum(album: IAlbum) = onIOThread { albumDao.upsertAlbum(album) }

    suspend fun upsertAlbumCombo(combo: IAlbumWithTracksCombo<IAlbum>) = onIOThread {
        albumDao.upsertAlbum(combo.album)
        albumDao.setAlbumTags(combo.album.albumId, combo.tags)
    }

    private suspend fun cleanImageCache() {
        // Get filenames the same way we do when we save images to cache:
        val uris = albumDao.listAlbumArtFullUris() + albumDao.listAlbumArtThumbnailUris()
        val hashes = uris.map { it.toUri().hashCode().toString() }

        context.imageCacheDir
            .listFiles { _, name -> hashes.find { name.startsWith(it) } == null }
            ?.forEach { it.delete() }
    }

    private fun mutableFlowSelectedAlbumIds(viewModelClass: String): MutableStateFlow<List<String>> =
        selectedAlbumIds[viewModelClass] ?: MutableStateFlow<List<String>>(emptyList()).also {
            selectedAlbumIds[viewModelClass] = it
        }
}
