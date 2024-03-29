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
import us.huseli.thoucylinder.enums.AvailabilityFilter
import us.huseli.thoucylinder.enums.SortOrder
import us.huseli.thoucylinder.enums.TrackSortParameter
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.views.TrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.deleteWithEmptyParentDirs
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackRepository @Inject constructor(database: Database, @ApplicationContext private val context: Context) {
    private val trackDao = database.trackDao()
    private val selectedTrackIds = mutableMapOf<String, MutableStateFlow<List<UUID>>>()

    suspend fun addToLibraryByAlbumId(albumIds: Collection<UUID>) =
        trackDao.setIsInLibraryByAlbumId(true, *albumIds.toTypedArray())

    suspend fun clearLocalUris(trackIds: Collection<UUID>) {
        if (trackIds.isNotEmpty()) trackDao.clearLocalUris(trackIds)
    }

    suspend fun clearTracks() = trackDao.clearTracks()

    suspend fun deleteTempTracks() = trackDao.deleteTempTracks()

    @WorkerThread
    fun deleteTrackFiles(tracks: Collection<Track>) = tracks
        .mapNotNull { it.getDocumentFile(context) }
        .forEach { it.deleteWithEmptyParentDirs(context) }

    suspend fun deleteTracks(tracks: Collection<Track>) {
        if (tracks.isNotEmpty()) trackDao.deleteTracks(*tracks.toTypedArray())
    }

    suspend fun deleteTracksByAlbumId(albumIds: Collection<UUID>) {
        if (albumIds.isNotEmpty()) trackDao.deleteTracksByAlbumId(*albumIds.toTypedArray())
    }

    fun flowSelectedTrackIds(viewModelClass: String): StateFlow<List<UUID>> =
        mutableFlowSelectedTrackIds(viewModelClass).asStateFlow()

    fun flowTagPojos(availabilityFilter: AvailabilityFilter) = trackDao.flowTagPojos(availabilityFilter)

    fun flowTrackComboPager(
        sortParameter: TrackSortParameter,
        sortOrder: SortOrder,
        searchTerm: String,
        tagNames: List<String>,
        availabilityFilter: AvailabilityFilter,
    ) = Pager(config = PagingConfig(pageSize = 100)) {
        trackDao.pageTrackCombos(sortParameter, sortOrder, searchTerm, tagNames, availabilityFilter)
    }

    suspend fun getLibraryTrackCount(): Int = trackDao.getLibraryTrackCount()

    suspend fun listRandomLibraryTrackCombos(
        limit: Int,
        exceptTrackIds: Collection<UUID>? = null,
        exceptSpotifyTrackIds: Collection<String>? = null,
    ): List<TrackCombo> = trackDao.listRandomLibraryTrackCombos(
        limit = limit,
        exceptTrackIds = exceptTrackIds ?: emptyList(),
        exceptSpotifyTrackIds = exceptSpotifyTrackIds ?: emptyList(),
    )

    suspend fun listTrackCombosByAlbumId(albumId: UUID) = trackDao.listTrackCombosByAlbumId(albumId)

    suspend fun listTrackCombosByArtistId(artistId: UUID) = trackDao.listTrackCombosByArtistId(artistId)

    suspend fun listTrackCombosById(trackIds: Collection<UUID>) =
        if (trackIds.isNotEmpty()) trackDao.listTrackCombosById(*trackIds.toTypedArray()) else emptyList()

    suspend fun listTrackLocalUris(): List<Uri> = trackDao.listLocalUris()

    suspend fun listTracks(): List<Track> = trackDao.listLibraryTracks()

    suspend fun listTracksByArtistId(artistId: UUID): List<Track> = trackDao.listTracksByArtistId(artistId)

    suspend fun listTracksById(trackIds: Collection<UUID>) =
        if (trackIds.isNotEmpty()) trackDao.listTracksById(*trackIds.toTypedArray()) else emptyList()

    fun pageTrackCombosByArtist(artistId: UUID): Pager<Int, TrackCombo> =
        Pager(config = PagingConfig(pageSize = 100)) { trackDao.pageTrackCombosByArtist(artistId) }

    suspend fun removeFromLibraryByAlbumId(albumIds: Collection<UUID>) =
        trackDao.setIsInLibraryByAlbumId(false, *albumIds.toTypedArray())

    fun selectTrackIds(selectionKey: String, trackIds: Iterable<UUID>) {
        mutableFlowSelectedTrackIds(selectionKey).also {
            val currentIds = it.value
            it.value += trackIds.filter { trackId -> !currentIds.contains(trackId) }
        }
    }

    suspend fun setAlbumTracks(albumId: UUID, tracks: Collection<Track>) = trackDao.setAlbumTracks(albumId, tracks)

    suspend fun setTrackSpotifyId(trackId: UUID, spotifyId: String) = trackDao.setTrackSpotifyId(trackId, spotifyId)

    fun toggleTrackIdSelected(selectionKey: String, trackId: UUID): Boolean {
        return mutableFlowSelectedTrackIds(selectionKey).let {
            if (it.value.contains(trackId)) {
                it.value -= trackId
                false
            } else {
                it.value += trackId
                true
            }
        }
    }

    fun unselectAllTrackIds(selectionKey: String) {
        mutableFlowSelectedTrackIds(selectionKey).value = emptyList()
    }

    fun unselectTrackIds(selectionKey: String, trackIds: Collection<UUID>) {
        mutableFlowSelectedTrackIds(selectionKey).value -= trackIds
    }

    suspend fun updateTrack(track: Track) = trackDao.updateTracks(track)

    suspend fun updateTracks(tracks: Collection<Track>) = trackDao.updateTracks(*tracks.toTypedArray())

    suspend fun upsertTrack(track: Track) = trackDao.upsertTracks(track)

    suspend fun upsertTracks(tracks: Collection<Track>) {
        if (tracks.isNotEmpty()) trackDao.upsertTracks(*tracks.toTypedArray())
    }


    /** PRIVATE METHODS *******************************************************/
    private fun mutableFlowSelectedTrackIds(viewModelClass: String): MutableStateFlow<List<UUID>> =
        selectedTrackIds[viewModelClass] ?: MutableStateFlow<List<UUID>>(emptyList()).also {
            selectedTrackIds[viewModelClass] = it
        }
}
