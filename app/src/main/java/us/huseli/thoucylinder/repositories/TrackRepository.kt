package us.huseli.thoucylinder.repositories

import android.content.Context
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.anggrayudi.storage.extension.openInputStream
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import linc.com.amplituda.Amplituda
import linc.com.amplituda.Compress
import us.huseli.retaintheme.extensions.pruneOrPad
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.views.TrackCombo
import us.huseli.thoucylinder.deleteWithEmptyParentDirs
import us.huseli.thoucylinder.enums.AvailabilityFilter
import us.huseli.thoucylinder.enums.SortOrder
import us.huseli.thoucylinder.enums.TrackSortParameter
import us.huseli.thoucylinder.interfaces.ILogger
import us.huseli.thoucylinder.isRemote
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

@Singleton
class TrackRepository @Inject constructor(database: Database, @ApplicationContext private val context: Context) :
    ILogger {
    private val trackDao = database.trackDao()
    private val selectedTrackIds = mutableMapOf<String, MutableStateFlow<List<String>>>()

    suspend fun addToLibrary(trackIds: Collection<String>) = trackDao.setIsInLibrary(true, *trackIds.toTypedArray())

    suspend fun addToLibraryByAlbumId(albumIds: Collection<String>) =
        trackDao.setIsInLibraryByAlbumId(true, *albumIds.toTypedArray())

    suspend fun clearLocalUris(trackIds: Collection<String>) {
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

    suspend fun deleteTracksByAlbumId(albumIds: Collection<String>) {
        if (albumIds.isNotEmpty()) trackDao.deleteTracksByAlbumId(*albumIds.toTypedArray())
    }

    fun flowSelectedTrackIds(viewModelClass: String): StateFlow<List<String>> =
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

    fun flowTrackCombosByAlbumId(albumId: String) = trackDao.flowTrackCombosByAlbumId(albumId)

    suspend fun getAlbumIdByTrackId(trackId: String): String? = trackDao.getAlbumIdByTrackId(trackId)

    suspend fun getAmplitudes(track: Track): ImmutableList<Int>? {
        track.amplitudeList?.also { return it }

        val lofiUri = track.lofiUri ?: return null
        val amplituda = Amplituda(context)
        val samplesPerSecond = track.durationMs?.div(1000)?.let { if (it < 100) ceil(100.0 / it).toInt() else 1 } ?: 1
        val comp = Compress.withParams(Compress.PEEK, samplesPerSecond)
        val output = lofiUri.toUri().takeIf { !it.isRemote }?.let { uri ->
            uri.openInputStream(context)?.use { amplituda.processAudio(it, comp) }
        } ?: amplituda.processAudio(lofiUri, comp)
        var amplitudes: ImmutableList<Int>? = null

        output.get(
            { result -> amplitudes = result.amplitudesAsList().pruneOrPad(100).toImmutableList() },
            { exception -> logError("amplitudes $lofiUri", exception) },
        )

        return amplitudes?.also {
            log("Got amplitudes for $track: max=${it.max()}, avg=${it.average()}")
            trackDao.setTrackAmplitudes(track.trackId, it.joinToString(","))
        }
    }

    suspend fun getLibraryTrackCount(): Int = trackDao.getLibraryTrackCount()

    fun getLocalAbsolutePath(track: Track): String? = track.getLocalAbsolutePath(context)

    suspend fun getTrackById(trackId: String): Track? = trackDao.getTrackById(trackId)

    suspend fun getTrackComboById(trackId: String): TrackCombo? = trackDao.getTrackComboById(trackId)

    suspend fun listNonAlbumTracks() = trackDao.listNonLocalTracks()

    suspend fun listRandomLibraryTrackCombos(
        limit: Int,
        exceptTrackIds: Collection<String>? = null,
        exceptSpotifyTrackIds: Collection<String>? = null,
    ): List<TrackCombo> = trackDao.listRandomLibraryTrackCombos(
        limit = limit,
        exceptTrackIds = exceptTrackIds ?: emptyList(),
        exceptSpotifyTrackIds = exceptSpotifyTrackIds ?: emptyList(),
    )

    suspend fun listTrackCombosByAlbumId(albumId: String) = trackDao.listTrackCombosByAlbumId(albumId)

    suspend fun listTrackCombosByArtistId(artistId: String) = trackDao.listTrackCombosByArtistId(artistId)

    suspend fun listTrackCombosById(trackIds: Collection<String>) =
        if (trackIds.isNotEmpty()) trackDao.listTrackCombosById(*trackIds.toTypedArray()) else emptyList()

    suspend fun listTrackIdsByAlbumId(albumIds: Collection<String>) =
        trackDao.listTrackIdsByAlbumId(*albumIds.toTypedArray()).toImmutableList()

    suspend fun listTrackLocalUris(): List<Uri> = trackDao.listLocalUris().map { it.toUri() }

    suspend fun listTrackIdsByArtistId(artistId: String): List<String> = trackDao.listTrackIdsByArtistId(artistId)

    suspend fun listTracksByAlbumId(albumId: String): List<Track> = trackDao.listTracksByAlbumId(albumId)

    suspend fun listTracksById(trackIds: Collection<String>) =
        if (trackIds.isNotEmpty()) trackDao.listTracksById(*trackIds.toTypedArray()) else emptyList()

    fun pageTrackCombosByArtist(artistId: String): Pager<Int, TrackCombo> =
        Pager(config = PagingConfig(pageSize = 100)) { trackDao.pageTrackCombosByArtist(artistId) }

    suspend fun removeFromLibraryByAlbumId(albumIds: Collection<String>) =
        trackDao.setIsInLibraryByAlbumId(false, *albumIds.toTypedArray())

    fun selectTrackIds(selectionKey: String, trackIds: Iterable<String>) {
        mutableFlowSelectedTrackIds(selectionKey).also {
            val currentIds = it.value
            it.value += trackIds.filter { trackId -> !currentIds.contains(trackId) }
        }
    }

    suspend fun setAlbumTracks(albumId: String, tracks: Collection<Track>) = trackDao.setAlbumTracks(albumId, tracks)

    suspend fun setTrackSpotifyId(trackId: String, spotifyId: String) = trackDao.setTrackSpotifyId(trackId, spotifyId)

    fun toggleTrackIdSelected(selectionKey: String, trackId: String): Boolean {
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

    fun unselectTrackIds(selectionKey: String, trackIds: Collection<String>) {
        mutableFlowSelectedTrackIds(selectionKey).value -= trackIds
    }

    suspend fun updateTrack(track: Track) = trackDao.updateTracks(track)

    suspend fun updateTracks(tracks: Collection<Track>) = trackDao.updateTracks(*tracks.toTypedArray())

    suspend fun upsertTrack(track: Track) = trackDao.upsertTracks(track)

    suspend fun upsertTracks(tracks: Collection<Track>) {
        if (tracks.isNotEmpty()) trackDao.upsertTracks(*tracks.toTypedArray())
    }


    /** PRIVATE METHODS *******************************************************/
    private fun mutableFlowSelectedTrackIds(viewModelClass: String): MutableStateFlow<List<String>> =
        selectedTrackIds[viewModelClass] ?: MutableStateFlow<List<String>>(emptyList()).also {
            selectedTrackIds[viewModelClass] = it
        }
}
