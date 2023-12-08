package us.huseli.thoucylinder.repositories

import android.content.Context
import android.net.Uri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import us.huseli.thoucylinder.AlbumSortParameter
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.SortOrder
import us.huseli.thoucylinder.TrackSortParameter
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.abstr.AbstractPlaylist
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.PlaylistTrack
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.AlbumPojo
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistPojo
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistTrackPojo
import us.huseli.thoucylinder.dataclasses.pojos.SpotifyAlbumPojo
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import us.huseli.thoucylinder.dataclasses.pojos.toPlaylistTracks
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepository @Inject constructor(
    private val database: Database,
    @ApplicationContext private val context: Context,
) {
    data class UndeleteAlbum(val albumPojo: AlbumWithTracksPojo, val spotifyPojo: SpotifyAlbumPojo?)

    private val trackDao = database.trackDao()
    private val albumDao = database.albumDao()
    private val playlistDao = database.playlistDao()
    private val artistDao = database.artistDao()
    private val spotifyDao = database.spotifyDao()

    // Keys = ViewModel class name:
    private val _selectedAlbums = mutableMapOf<String, MutableStateFlow<List<Album>>>()
    private val _selectedTrackPojos = mutableMapOf<String, MutableStateFlow<List<TrackPojo>>>()
    private val _undeleteAlbums = mutableMapOf<UUID, UndeleteAlbum>()

    val artistPojos = artistDao.flowArtistPojos()
    val playlists = playlistDao.flowPojos()

    suspend fun addSelectionToPlaylist(
        selection: Selection,
        playlist: AbstractPlaylist,
        offset: Int
    ) = database.withTransaction {
        val tracks = getTracksFromSelection(selection)
        val playlistTracks = tracks.mapIndexed { index, track ->
            PlaylistTrack(
                playlistId = playlist.playlistId,
                trackId = track.trackId,
                position = index + offset,
            )
        }
        val unsavedTracks = tracks.filter { !it.isInLibrary }

        trackDao.insertTracks(unsavedTracks)
        playlistDao.insertTracks(*playlistTracks.toTypedArray())
        playlistDao.touchPlaylist(playlist.playlistId)
    }

    suspend fun deleteAlbums(albums: Collection<Album>) = albumDao.deleteAlbums(*albums.toTypedArray())

    suspend fun deleteAlbumWithTracks(album: Album) = database.withTransaction {
        albumDao.getAlbumWithTracks(album.albumId)?.also {
            _undeleteAlbums[album.albumId] = UndeleteAlbum(it, spotifyDao.getSpotifyAlbumPojo(it.album.albumId))
        }
        trackDao.deleteTracksByAlbumId(album.albumId)
        deleteAlbums(listOf(album))
    }

    suspend fun deletePlaylist(playlist: Playlist) = playlistDao.deletePlaylist(playlist)

    suspend fun deleteTempTracksAndAlbums() {
        trackDao.deleteTempTracks()
        albumDao.deleteTempAlbums()
    }

    suspend fun deleteTracks(tracks: List<Track>) = trackDao.deleteTracks(*tracks.toTypedArray())

    fun flowAlbumPojos(sortParameter: AlbumSortParameter, sortOrder: SortOrder, searchTerm: String) =
        albumDao.flowAlbumPojos(sortParameter = sortParameter, sortOrder = sortOrder, searchTerm = searchTerm)

    fun flowAlbumPojosByArtist(artist: String) = albumDao.flowAlbumPojosByArtist(artist)

    fun flowAlbumWithTracks(albumId: UUID): Flow<AlbumWithTracksPojo?> =
        albumDao.flowAlbumWithTracks(albumId).map { pojo -> pojo?.copy(tracks = pojo.tracks.sorted()) }

    fun flowTrackPojoPager(sortParameter: TrackSortParameter, sortOrder: SortOrder, searchTerm: String) =
        Pager(config = PagingConfig(pageSize = 100)) { trackDao.pageTrackPojos(sortParameter, sortOrder, searchTerm) }

    suspend fun getAlbumWithTracks(albumId: UUID): AlbumWithTracksPojo? =
        albumDao.getAlbumWithTracks(albumId)?.let { pojo -> pojo.copy(tracks = pojo.tracks.sorted()) }

    fun getSelectedAlbumFlow(viewModelClass: String): StateFlow<List<Album>> =
        _selectedAlbums[viewModelClass] ?: MutableStateFlow<List<Album>>(emptyList()).also {
            _selectedAlbums[viewModelClass] = it
        }

    fun getSelectedTrackPojoFlow(viewModelClass: String): StateFlow<List<TrackPojo>> =
        _selectedTrackPojos[viewModelClass] ?: MutableStateFlow<List<TrackPojo>>(emptyList()).also {
            _selectedTrackPojos[viewModelClass] = it
        }

    suspend fun getTrackAlbum(track: Track): Album? = track.albumId?.let { albumDao.getAlbum(it) }

    suspend fun insertPlaylist(playlist: Playlist, selection: Selection? = null) = database.withTransaction {
        playlistDao.insertPlaylist(playlist)
        if (selection != null) addSelectionToPlaylist(selection, playlist, 0)
    }

    suspend fun insertPlaylist(pojo: PlaylistPojo, trackPojos: List<PlaylistTrackPojo>) = database.withTransaction {
        playlistDao.insertPlaylist(pojo.toPlaylist())
        playlistDao.insertTracks(*trackPojos.map { it.toPlaylistTrack() }.toTypedArray())
    }

    suspend fun insertTempAlbumsWithTracks(pojos: List<AlbumWithTracksPojo>) {
        albumDao.insertTempAlbums(pojos.map { it.album })
        trackDao.insertTempTracks(pojos.flatMap { it.tracks })
    }

    suspend fun listAlbums(): List<Album> = albumDao.listAlbums()

    suspend fun listAlbumTrackPojos(albumIds: List<UUID>): List<TrackPojo> = albumDao.listTrackPojos(albumIds)

    suspend fun listImageUris(): Set<Uri> = (albumDao.listImageUris() + trackDao.listImageUris()).toSet()

    suspend fun listPlaylistAlbums(playlistId: UUID): List<Album> = playlistDao.listAlbums(playlistId)

    suspend fun listPlaylistTracks(playlistId: UUID): List<PlaylistTrackPojo> = playlistDao.listTracks(playlistId)

    suspend fun listPlaylistTracksBetween(
        playlistId: UUID,
        from: PlaylistTrackPojo,
        to: PlaylistTrackPojo,
    ): List<PlaylistTrackPojo> = playlistDao.listTracksBetween(playlistId, from, to)

    suspend fun listTracks(): List<Track> = trackDao.listTracks()

    suspend fun listTrackPojosBetween(from: AbstractTrackPojo, to: AbstractTrackPojo) =
        trackDao.listTrackPojosBetween(from, to)

    fun pageTrackPojosByArtist(artist: String): Pager<Int, TrackPojo> =
        Pager(config = PagingConfig(pageSize = 100)) { trackDao.pageTrackPojosByArtist(artist) }

    fun pageTrackPojosByPlaylistId(playlistId: UUID): Pager<Int, PlaylistTrackPojo> =
        Pager(config = PagingConfig(pageSize = 100)) { playlistDao.pageTracks(playlistId) }

    suspend fun removePlaylistTracks(pojos: List<PlaylistTrackPojo>) = database.withTransaction {
        playlistDao.deletePlaylistTracks(*pojos.toPlaylistTracks().toTypedArray())
        pojos.map { it.playlist.playlistId }.toSet().forEach { playlistId -> playlistDao.touchPlaylist(playlistId) }
    }

    suspend fun saveAlbum(album: Album) = database.withTransaction {
        if (albumDao.albumExists(album.albumId)) albumDao.updateAlbums(album)
        else albumDao.insertAlbum(album)
    }

    suspend fun saveAlbumWithTracks(pojo: AlbumWithTracksPojo) = database.withTransaction {
        /** Does not save pojo.spotifyAlbum! Do that in SpotifyRepository. */
        val albumArt = pojo.saveMediaStoreImage(context)
        val album = pojo.album.copy(albumArt = albumArt)

        if (albumDao.albumExists(album.albumId)) {
            albumDao.updateAlbums(album)
            trackDao.deleteTracksByAlbumId(album.albumId)
            albumDao.clearAlbumGenres(album.albumId)
            albumDao.clearAlbumStyles(album.albumId)
        } else albumDao.insertAlbum(album)

        if (pojo.tracks.isNotEmpty()) trackDao.insertTracks(
            pojo.tracks.map { track ->
                track.copy(
                    albumId = album.albumId,
                    image = (track.image ?: track.youtubeVideo?.saveMediaStoreImage(context)),
                )
            }
        )
        if (pojo.genres.isNotEmpty()) albumDao.insertAlbumGenres(pojo)
        if (pojo.styles.isNotEmpty()) albumDao.insertAlbumStyles(pojo)
    }

    fun searchAlbumPojos(query: String): Flow<List<AlbumPojo>> = albumDao.searchAlbumPojos(query)

    fun searchTrackPojos(query: String): Pager<Int, TrackPojo> =
        Pager(config = PagingConfig(pageSize = 100)) { trackDao.searchTrackPojos(query) }

    fun selectAlbums(selectionKey: String, albums: List<Album>) {
        _selectedAlbums[selectionKey]?.also {
            val currentIds = it.value.map { album -> album.albumId }
            it.value += albums.filter { album -> !currentIds.contains(album.albumId) }
        }
    }

    fun selectTrackPojos(selectionKey: String, tracks: List<TrackPojo>) {
        _selectedTrackPojos[selectionKey]?.also {
            val currentIds = it.value.map { track -> track.trackId }
            it.value += tracks.filter { track -> !currentIds.contains(track.trackId) }
        }
    }

    fun toggleAlbumSelected(selectionKey: String, album: Album) {
        _selectedAlbums[selectionKey]?.also {
            if (it.value.contains(album))
                it.value -= album
            else
                it.value += album
        }
    }

    fun toggleTrackPojoSelected(selectionKey: String, track: TrackPojo) {
        _selectedTrackPojos[selectionKey]?.also {
            if (it.value.contains(track))
                it.value -= track
            else
                it.value += track
        }
    }

    suspend fun undeleteAlbumWithTracks(album: Album) {
        _undeleteAlbums.remove(album.albumId)?.also { pojos ->
            albumDao.insertAlbum(pojos.albumPojo.album)
            trackDao.insertTracks(pojos.albumPojo.tracks)
            pojos.spotifyPojo?.also { spotifyDao.upsertSpotifyAlbumPojo(it) }
        }
    }

    fun unselectAllAlbums(selectionKey: String) {
        _selectedAlbums[selectionKey]?.also { it.value = emptyList() }
    }

    fun unselectAllTrackPojos(selectionKey: String) {
        _selectedTrackPojos[selectionKey]?.also { it.value = emptyList() }
    }

    suspend fun updateAlbum(album: Album) = albumDao.updateAlbums(album)

    suspend fun updateAlbums(albums: Collection<Album>) = albumDao.updateAlbums(*albums.toTypedArray())

    suspend fun updateTrack(track: Track) = trackDao.updateTracks(track)

    suspend fun updateTracks(tracks: Collection<Track>) = trackDao.updateTracks(*tracks.toTypedArray())

    private suspend fun getTracksFromSelection(selection: Selection): List<Track> {
        val tracks = mutableListOf<Track>()

        tracks.addAll(selection.tracks)
        tracks.addAll(selection.queueTracks.map { it.track })
        tracks.addAll(selection.albums.flatMap { albumDao.listTracks(it.albumId) })

        return tracks
    }
}
