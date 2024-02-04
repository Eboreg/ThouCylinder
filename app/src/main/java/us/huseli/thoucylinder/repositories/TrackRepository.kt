package us.huseli.thoucylinder.repositories

import android.content.Context
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.paging.Pager
import androidx.paging.PagingConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.thoucylinder.SortOrder
import us.huseli.thoucylinder.TrackSortParameter
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import us.huseli.thoucylinder.deleteWithEmptyParentDirs
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackRepository @Inject constructor(database: Database, @ApplicationContext private val context: Context) {
    private val trackDao = database.trackDao()

    private val _selectedTrackPojos = mutableMapOf<String, MutableStateFlow<List<TrackPojo>>>()

    suspend fun addToLibraryByAlbumId(albumId: UUID) = trackDao.setIsInLibraryByAlbumId(albumId, true)

    suspend fun deleteTempTracks() = trackDao.deleteTempTracks()

    @WorkerThread
    fun deleteTrackFiles(tracks: Collection<Track>) = tracks
        .mapNotNull { it.getDocumentFile(context) }
        .forEach { it.deleteWithEmptyParentDirs(context) }

    suspend fun deleteTracks(tracks: Collection<Track>) {
        if (tracks.isNotEmpty()) trackDao.deleteTracks(*tracks.toTypedArray())
    }

    suspend fun deleteTracksByAlbumId(albumId: UUID) = trackDao.deleteTracksByAlbumId(albumId)

    suspend fun deleteTracksByAlbumId(albumIds: Collection<UUID>) =
        trackDao.deleteTracksByAlbumId(*albumIds.toTypedArray())

    fun flowSelectedTrackPojos(viewModelClass: String): StateFlow<List<TrackPojo>> =
        mutableFlowSelectedTrackPojos(viewModelClass).asStateFlow()

    fun flowTrackPojoPager(sortParameter: TrackSortParameter, sortOrder: SortOrder, searchTerm: String) =
        Pager(config = PagingConfig(pageSize = 100)) { trackDao.pageTrackPojos(sortParameter, sortOrder, searchTerm) }

    fun flowTrackPojosByAlbumId(albumId: UUID) = trackDao.flowTrackPojosByAlbumId(albumId)

    suspend fun insertTracks(tracks: Collection<Track>) {
        if (tracks.isNotEmpty()) trackDao.insertTracks(*tracks.toTypedArray())
    }

    suspend fun listTracks(): List<Track> = trackDao.listLibraryTracks()

    suspend fun listTrackLocalUris(): List<Uri> = trackDao.listLocalUris()

    fun pageTrackPojosByArtist(artist: String): Pager<Int, TrackPojo> =
        Pager(config = PagingConfig(pageSize = 100)) { trackDao.pageTrackPojosByArtist(artist) }

    fun selectTrackPojos(selectionKey: String, pojos: Iterable<TrackPojo>) {
        mutableFlowSelectedTrackPojos(selectionKey).also {
            val currentIds = it.value.map { pojo -> pojo.track.trackId }
            it.value += pojos.filter { pojo -> !currentIds.contains(pojo.track.trackId) }
        }
    }

    fun toggleTrackPojoSelected(selectionKey: String, track: TrackPojo): Boolean {
        return mutableFlowSelectedTrackPojos(selectionKey).let {
            if (it.value.contains(track)) {
                it.value -= track
                false
            } else {
                it.value += track
                true
            }
        }
    }

    fun unselectAllTrackPojos(selectionKey: String) {
        mutableFlowSelectedTrackPojos(selectionKey).value = emptyList()
    }

    suspend fun updateTrack(track: Track) = updateTracks(listOf(track))

    suspend fun updateTracks(tracks: Collection<Track>) {
        if (tracks.isNotEmpty()) trackDao.updateTracks(*tracks.toTypedArray())
    }


    /** PRIVATE METHODS *******************************************************/
    private fun mutableFlowSelectedTrackPojos(viewModelClass: String): MutableStateFlow<List<TrackPojo>> =
        _selectedTrackPojos[viewModelClass] ?: MutableStateFlow<List<TrackPojo>>(emptyList()).also {
            _selectedTrackPojos[viewModelClass] = it
        }
}
