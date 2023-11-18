@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbum
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbumArtist
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbumGenre
import us.huseli.thoucylinder.dataclasses.entities.SpotifyArtist
import us.huseli.thoucylinder.dataclasses.entities.SpotifyTrack
import us.huseli.thoucylinder.dataclasses.entities.SpotifyTrackArtist
import us.huseli.thoucylinder.dataclasses.pojos.SpotifyAlbumPojo
import us.huseli.thoucylinder.dataclasses.pojos.SpotifyTrackPojo
import java.util.UUID

@Dao
interface SpotifyDao {
    @Query("DELETE FROM SpotifyAlbumArtist WHERE SpotifyAlbumArtist_albumId = :albumId AND SpotifyAlbumArtist_artistId NOT IN (:except)")
    suspend fun _deleteSpotifyAlbumArtists(albumId: String, except: List<String> = emptyList())

    @Query("DELETE FROM SpotifyAlbumGenre WHERE SpotifyAlbumGenre_albumId = :albumId AND SpotifyAlbumGenre_genreName NOT IN (:except)")
    suspend fun _deleteSpotifyAlbumGenres(albumId: String, except: List<String> = emptyList())

    @Query("DELETE FROM SpotifyTrack WHERE SpotifyTrack_spotifyAlbumId = :albumId AND SpotifyTrack_id NOT IN (:except)")
    suspend fun _deleteSpotifyTracks(albumId: String, except: List<String> = emptyList())

    @Query("SELECT * FROM SpotifyAlbumArtist")
    fun _flowSpotifyAlbumArtists(): Flow<List<SpotifyAlbumArtist>>

    @Query("SELECT * FROM SpotifyAlbumGenre")
    fun _flowSpotifyAlbumGenres(): Flow<List<SpotifyAlbumGenre>>

    @Query("SELECT * FROM SpotifyAlbum")
    fun _flowSpotifyAlbums(): Flow<List<SpotifyAlbum>>

    @Query("SELECT * FROM SpotifyArtist")
    fun _flowSpotifyArtists(): Flow<List<SpotifyArtist>>

    @Query("SELECT * FROM SpotifyTrackArtist")
    fun _flowSpotifyTrackArtists(): Flow<List<SpotifyTrackArtist>>

    fun _flowSpotifyTrackPojos(): Flow<List<SpotifyTrackPojo>> = combine(
        _flowSpotifyTracks(),
        _flowSpotifyTrackArtists(),
        _flowSpotifyArtists(),
    ) { tracks, trackArtists, artists ->
        tracks.map { track ->
            SpotifyTrackPojo(
                track = track,
                artists = artists.filter { artist ->
                    trackArtists.any { it.trackId == track.id && it.artistId == artist.id }
                },
            )
        }
    }

    @Query("SELECT * FROM SpotifyTrack")
    fun _flowSpotifyTracks(): Flow<List<SpotifyTrack>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun _insertGenres(vararg genres: Genre)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun _insertSpotifyAlbumArtists(vararg albumArtists: SpotifyAlbumArtist)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun _insertSpotifyAlbumGenres(vararg albumGenres: SpotifyAlbumGenre)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun _insertSpotifyArtists(vararg artist: SpotifyArtist)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun _insertSpotifyTrackArtists(vararg trackArtists: SpotifyTrackArtist)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun _insertSpotifyTracks(vararg track: SpotifyTrack)

    @Query("SELECT EXISTS(SELECT SpotifyAlbum_id FROM SpotifyAlbum WHERE SpotifyAlbum_id = :albumId)")
    suspend fun _spotifyAlbumExists(albumId: String): Boolean

    @Upsert
    suspend fun _upsertSpotifyAlbums(vararg albums: SpotifyAlbum)

    // TODO: Maybe not use?
    fun flowSpotifyAlbumPojos(): Flow<List<SpotifyAlbumPojo>> = combine(
        _flowSpotifyAlbums(),
        _flowSpotifyAlbumGenres(),
        _flowSpotifyAlbumArtists(),
        _flowSpotifyArtists(),
        _flowSpotifyTrackPojos(),
    ) { albums, genres, albumArtists, artists, trackPojos ->
        albums.map { album ->
            SpotifyAlbumPojo(
                spotifyAlbum = album,
                artists = artists.filter { artist ->
                    albumArtists.any { it.albumId == album.id && it.artistId == artist.id }
                },
                genres = genres.filter { it.albumId == album.id }.map { Genre(it.genreName) },
                spotifyTrackPojos = trackPojos.filter { it.track.albumId == album.id },
            )
        }
    }

    @Transaction
    @Query("SELECT * FROM SpotifyAlbum WHERE SpotifyAlbum_albumId = :albumId")
    suspend fun getSpotifyAlbumPojo(albumId: UUID): SpotifyAlbumPojo?

    @Query("SELECT DISTINCT SpotifyAlbum_id FROM SpotifyAlbum s JOIN Album a ON a.Album_albumId = s.SpotifyAlbum_albumId WHERE a.Album_isInLibrary = 1")
    suspend fun listImportedAlbumIds(): List<String>

    @Transaction
    suspend fun upsertSpotifyAlbumPojo(pojo: SpotifyAlbumPojo) {
        val albumArtists = pojo.artists.toSet()
        val trackArtists = pojo.spotifyTrackPojos.flatMap { it.artists }.toSet()
        val artists = albumArtists.plus(trackArtists).toSet()

        if (_spotifyAlbumExists(pojo.spotifyAlbum.id)) {
            _deleteSpotifyAlbumArtists(pojo.spotifyAlbum.id, except = pojo.artists.map { it.id })
            _deleteSpotifyAlbumGenres(pojo.spotifyAlbum.id, except = pojo.genres.map { it.genreName })
            _deleteSpotifyTracks(pojo.spotifyAlbum.id, except = pojo.spotifyTrackPojos.map { it.track.id })
        }
        _upsertSpotifyAlbums(pojo.spotifyAlbum)
        _insertSpotifyArtists(*artists.toTypedArray())
        _insertSpotifyAlbumArtists(
            *albumArtists
                .map { SpotifyAlbumArtist(artistId = it.id, albumId = pojo.spotifyAlbum.id) }
                .toTypedArray()
        )
        _insertGenres(*pojo.genres.toTypedArray())
        _insertSpotifyAlbumGenres(
            *pojo.genres
                .map { SpotifyAlbumGenre(albumId = pojo.spotifyAlbum.id, genreName = it.genreName) }
                .toTypedArray()
        )
        _insertSpotifyTracks(*pojo.spotifyTrackPojos.map { it.track }.toTypedArray())
        _insertSpotifyTrackArtists(
            *pojo.spotifyTrackPojos.flatMap { trackPojo ->
                trackPojo.artists.map { SpotifyTrackArtist(trackId = trackPojo.track.id, artistId = it.id) }
            }.toTypedArray()
        )
    }
}
