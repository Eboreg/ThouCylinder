@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbum
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbumArtist
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbumGenre
import us.huseli.thoucylinder.dataclasses.entities.SpotifyArtist
import us.huseli.thoucylinder.dataclasses.entities.SpotifyTrack
import us.huseli.thoucylinder.dataclasses.entities.SpotifyTrackArtist
import us.huseli.thoucylinder.dataclasses.combos.SpotifyAlbumCombo
import java.util.UUID

@Dao
interface SpotifyDao {
    @Query("DELETE FROM SpotifyAlbumArtist WHERE SpotifyAlbumArtist_albumId = :albumId AND SpotifyAlbumArtist_artistId NOT IN (:except)")
    suspend fun _deleteSpotifyAlbumArtists(albumId: String, except: List<String> = emptyList())

    @Query("DELETE FROM SpotifyAlbumGenre WHERE SpotifyAlbumGenre_albumId = :albumId AND SpotifyAlbumGenre_genreName NOT IN (:except)")
    suspend fun _deleteSpotifyAlbumGenres(albumId: String, except: List<String> = emptyList())

    @Query("DELETE FROM SpotifyTrack WHERE SpotifyTrack_spotifyAlbumId = :albumId AND SpotifyTrack_id NOT IN (:except)")
    suspend fun _deleteSpotifyTracks(albumId: String, except: List<String> = emptyList())

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

    @Query("SELECT * FROM SpotifyAlbum WHERE SpotifyAlbum_albumId = :albumId")
    suspend fun getSpotifyAlbum(albumId: UUID): SpotifyAlbum?

    @Query(
        """
        SELECT DISTINCT SpotifyAlbum_id FROM SpotifyAlbum
            JOIN Album ON Album_albumId = SpotifyAlbum_albumId
        WHERE Album_isInLibrary = 1 AND Album_isDeleted = 0
        """
    )
    suspend fun listImportedAlbumIds(): List<String>

    @Transaction
    suspend fun upsertSpotifyAlbumCombo(combo: SpotifyAlbumCombo) {
        val albumArtists = combo.artists.toSet()
        val trackArtists = combo.spotifyTrackCombos.flatMap { it.artists }.toSet()
        val artists = albumArtists.plus(trackArtists).toSet()

        if (_spotifyAlbumExists(combo.spotifyAlbum.id)) {
            _deleteSpotifyAlbumArtists(combo.spotifyAlbum.id, except = combo.artists.map { it.id })
            _deleteSpotifyAlbumGenres(combo.spotifyAlbum.id, except = combo.genres.map { it.genreName })
            _deleteSpotifyTracks(combo.spotifyAlbum.id, except = combo.spotifyTrackCombos.map { it.track.id })
        }
        _upsertSpotifyAlbums(combo.spotifyAlbum)
        _insertSpotifyArtists(*artists.toTypedArray())
        _insertSpotifyAlbumArtists(
            *albumArtists
                .map { SpotifyAlbumArtist(artistId = it.id, albumId = combo.spotifyAlbum.id) }
                .toTypedArray()
        )
        _insertGenres(*combo.genres.toTypedArray())
        _insertSpotifyAlbumGenres(
            *combo.genres
                .map { SpotifyAlbumGenre(albumId = combo.spotifyAlbum.id, genreName = it.genreName) }
                .toTypedArray()
        )
        _insertSpotifyTracks(*combo.spotifyTrackCombos.map { it.track }.toTypedArray())
        _insertSpotifyTrackArtists(
            *combo.spotifyTrackCombos.flatMap { trackCombo ->
                trackCombo.artists.map { SpotifyTrackArtist(trackId = trackCombo.track.id, artistId = it.id) }
            }.toTypedArray()
        )
    }
}
