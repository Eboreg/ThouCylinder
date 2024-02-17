package us.huseli.thoucylinder.repositories

import android.content.Context
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.compose.ui.graphics.ImageBitmap
import androidx.paging.Pager
import androidx.paging.PagingConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.thoucylinder.MutexCache
import us.huseli.thoucylinder.SortOrder
import us.huseli.thoucylinder.TrackSortParameter
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.combos.TrackCombo
import us.huseli.thoucylinder.deleteWithEmptyParentDirs
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackRepository @Inject constructor(database: Database, @ApplicationContext private val context: Context) {
    private val trackDao = database.trackDao()
    private val selectedTrackCombos = mutableMapOf<String, MutableStateFlow<List<TrackCombo>>>()

    val thumbnailCache = MutexCache<MediaStoreImage, ImageBitmap> {
        it.getThumbnailImageBitmap(context)
    }

    suspend fun addToLibraryByAlbumId(albumId: UUID) = trackDao.setIsInLibraryByAlbumId(albumId, true)

    suspend fun clearLocalUris(trackIds: Collection<UUID>) = trackDao.clearLocalUris(trackIds)

    suspend fun deleteTempTracks() = trackDao.deleteTempTracks()

    @WorkerThread
    fun deleteTrackFiles(tracks: Collection<Track>) = tracks
        .mapNotNull { it.getDocumentFile(context) }
        .forEach { it.deleteWithEmptyParentDirs(context) }

    suspend fun deleteTracks(tracks: Collection<Track>) {
        if (tracks.isNotEmpty()) trackDao.deleteTracks(*tracks.toTypedArray())
    }

    suspend fun deleteTracksByAlbumId(albumIds: Collection<UUID>) =
        trackDao.deleteTracksByAlbumId(*albumIds.toTypedArray())

    fun flowSelectedTrackCombos(viewModelClass: String): StateFlow<List<TrackCombo>> =
        mutableFlowSelectedTrackCombos(viewModelClass).asStateFlow()

    fun flowTrackComboPager(sortParameter: TrackSortParameter, sortOrder: SortOrder, searchTerm: String) =
        Pager(config = PagingConfig(pageSize = 100)) { trackDao.pageTrackCombos(sortParameter, sortOrder, searchTerm) }

    fun flowTrackCombosByAlbumId(albumId: UUID) = trackDao.flowTrackCombosByAlbumId(albumId)

    suspend fun insertTracks(tracks: Collection<Track>) {
        if (tracks.isNotEmpty()) trackDao.insertTracks(*tracks.toTypedArray())
    }

    suspend fun listTracks(): List<Track> = trackDao.listLibraryTracks()

    suspend fun listTrackLocalUris(): List<Uri> = trackDao.listLocalUris()

    fun pageTrackCombosByArtist(artist: String): Pager<Int, TrackCombo> =
        Pager(config = PagingConfig(pageSize = 100)) { trackDao.pageTrackCombosByArtist(artist) }

    fun selectTrackCombos(selectionKey: String, combos: Iterable<TrackCombo>) {
        mutableFlowSelectedTrackCombos(selectionKey).also {
            val currentIds = it.value.map { combo -> combo.track.trackId }
            it.value += combos.filter { combo -> !currentIds.contains(combo.track.trackId) }
        }
    }

    fun toggleTrackComboSelected(selectionKey: String, track: TrackCombo): Boolean {
        return mutableFlowSelectedTrackCombos(selectionKey).let {
            if (it.value.contains(track)) {
                it.value -= track
                false
            } else {
                it.value += track
                true
            }
        }
    }

    fun unselectAllTrackCombos(selectionKey: String) {
        mutableFlowSelectedTrackCombos(selectionKey).value = emptyList()
    }

    suspend fun updateTrack(track: Track) = updateTracks(listOf(track))

    suspend fun updateTracks(tracks: Collection<Track>) {
        if (tracks.isNotEmpty()) trackDao.updateTracks(*tracks.toTypedArray())
    }


    /** PRIVATE METHODS *******************************************************/
    private fun mutableFlowSelectedTrackCombos(viewModelClass: String): MutableStateFlow<List<TrackCombo>> =
        selectedTrackCombos[viewModelClass] ?: MutableStateFlow<List<TrackCombo>>(emptyList()).also {
            selectedTrackCombos[viewModelClass] = it
        }
}
