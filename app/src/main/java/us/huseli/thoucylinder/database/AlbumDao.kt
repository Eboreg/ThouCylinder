@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import android.net.Uri
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.AlbumGenre
import us.huseli.thoucylinder.dataclasses.entities.AlbumStyle
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbum
import us.huseli.thoucylinder.dataclasses.entities.Style
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.AlbumPojo
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import java.util.UUID

@Dao
interface AlbumDao {
    /** Pseudo-private methods ***********************************************/
    @Query("SELECT * FROM AlbumGenre")
    fun _flowAlbumGenres(): Flow<List<AlbumGenre>>

    @Query("SELECT * FROM AlbumStyle")
    fun _flowAlbumStyles(): Flow<List<AlbumStyle>>

    @Query(
        """
        SELECT a.*, 
            SUM(COALESCE(Track_metadata_durationMs, Track_youtubeVideo_durationMs, Track_youtubeVideo_metadata_durationMs)) AS durationMs,
            MIN(Track_year) AS minYear, MAX(Track_year) AS maxYear, COUNT(Track_trackId) AS trackCount
        FROM Album a LEFT JOIN Track t ON Album_albumId = Track_albumId 
        WHERE Album_isInLibrary = 1
        GROUP BY Album_albumId
        ORDER BY LOWER(Album_artist), LOWER(Album_title)
        """
    )
    fun _flowAlbumPojos(): Flow<List<AlbumPojo>>

    @Query("SELECT * FROM Album WHERE Album_albumId = :albumId")
    @Transaction
    fun _flowAlbumWithTracks(albumId: UUID): Flow<AlbumWithTracksPojo?>

    @Query("SELECT * FROM SpotifyAlbum WHERE SpotifyAlbum_albumId = :albumId")
    fun _flowSpotifyAlbum(albumId: UUID): Flow<SpotifyAlbum?>

    @Query("SELECT * FROM SpotifyAlbum")
    fun _flowSpotifyAlbums(): Flow<List<SpotifyAlbum>>

    @Query("SELECT Genre.* FROM Genre JOIN AlbumGenre ON Genre_genreName = AlbumGenre_genreName WHERE AlbumGenre_albumId = :albumId")
    fun _flowGenres(albumId: UUID): Flow<List<Genre>>

    @Query("SELECT Style.* FROM Style JOIN AlbumStyle ON Style_styleName = AlbumStyle_styleName WHERE AlbumStyle_albumId = :albumId")
    fun _flowStyles(albumId: UUID): Flow<List<Style>>

    @Query("SELECT * FROM Album WHERE Album_albumId = :albumId")
    @Transaction
    suspend fun _getAlbumWithTracks(albumId: UUID): AlbumWithTracksPojo?

    @Query("SELECT * FROM SpotifyAlbum WHERE SpotifyAlbum_albumId = :albumId")
    suspend fun _getSpotifyAlbum(albumId: UUID): SpotifyAlbum?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun _insertAlbums(vararg albums: Album)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun _insertAlbumGenres(vararg albumGenres: AlbumGenre)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun _insertAlbumStyles(vararg albumStyles: AlbumStyle)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun _insertGenres(vararg genres: Genre)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun _insertStyles(vararg styles: Style)

    @Query("SELECT Genre.* FROM Genre JOIN AlbumGenre ON Genre_genreName = AlbumGenre_genreName WHERE AlbumGenre_albumId = :albumId")
    suspend fun _listGenres(albumId: UUID): List<Genre>

    @Query("SELECT Style.* FROM Style JOIN AlbumStyle ON Style_styleName = AlbumStyle_styleName WHERE AlbumStyle_albumId = :albumId")
    suspend fun _listStyles(albumId: UUID): List<Style>

    @RawQuery(observedEntities = [Album::class, Track::class])
    fun _searchAlbums(query: SupportSQLiteQuery): Flow<List<AlbumPojo>>

    @Update
    suspend fun _updateAlbums(vararg albums: Album)

    /** Public methods *******************************************************/
    @Query("SELECT EXISTS(SELECT Album_albumId FROM Album WHERE Album_albumId = :albumId)")
    suspend fun albumExists(albumId: UUID): Boolean

    @Query("DELETE FROM AlbumGenre WHERE AlbumGenre_albumId = :albumId")
    suspend fun clearAlbumGenres(albumId: UUID)

    @Query("DELETE FROM AlbumStyle WHERE AlbumStyle_albumId = :albumId")
    suspend fun clearAlbumStyles(albumId: UUID)

    @Query("DELETE FROM Album")
    suspend fun clearAlbums()

    @Delete
    suspend fun deleteAlbums(vararg albums: Album)

    @Query("DELETE FROM Album WHERE Album_isInLibrary = 0")
    suspend fun deleteTempAlbums()

    fun flowAlbumPojos(): Flow<List<AlbumPojo>> =
        combine(
            _flowAlbumPojos(),
            _flowAlbumGenres(),
            _flowAlbumStyles(),
            _flowSpotifyAlbums(),
        ) { pojos, genres, styles, spotifyAlbums ->
            pojos.map { pojo ->
                pojo.copy(
                    genres = genres.filter { it.albumId == pojo.album.albumId }.map { Genre(it.genreName) },
                    styles = styles.filter { it.albumId == pojo.album.albumId }.map { Style(it.styleName) },
                    spotifyAlbum = spotifyAlbums.find { it.albumId == pojo.album.albumId },
                )
            }
        }

    fun flowAlbumWithTracks(albumId: UUID): Flow<AlbumWithTracksPojo?> = combine(
        _flowAlbumWithTracks(albumId),
        _flowGenres(albumId),
        _flowStyles(albumId),
        _flowSpotifyAlbum(albumId)
    ) { pojo, genres, styles, spotifyPojo ->
        pojo?.copy(genres = genres, styles = styles, spotifyAlbum = spotifyPojo)
    }

    @Query("SELECT * FROM Album WHERE Album_albumId = :albumId")
    suspend fun getAlbum(albumId: UUID): Album?

    @Transaction
    suspend fun getAlbumWithTracks(albumId: UUID): AlbumWithTracksPojo? = _getAlbumWithTracks(albumId)?.copy(
        genres = _listGenres(albumId),
        styles = _listStyles(albumId),
        spotifyAlbum = _getSpotifyAlbum(albumId),
    )

    suspend fun insertAlbumGenres(pojo: AbstractAlbumPojo) {
        _insertGenres(*pojo.genres.toTypedArray())
        _insertAlbumGenres(*pojo.genres.map {
            AlbumGenre(albumId = pojo.album.albumId, genreName = it.genreName)
        }.toTypedArray())
    }

    suspend fun insertAlbumStyles(pojo: AbstractAlbumPojo) {
        _insertStyles(*pojo.styles.toTypedArray())
        _insertAlbumStyles(*pojo.styles.map {
            AlbumStyle(albumId = pojo.album.albumId, styleName = it.styleName)
        }.toTypedArray())
    }

    suspend fun insertAlbum(album: Album) = _insertAlbums(album.copy(isInLibrary = true))

    suspend fun insertTempAlbums(albums: List<Album>) =
        _insertAlbums(*albums.map { it.copy(isInLibrary = false) }.toTypedArray())

    @Query("SELECT * FROM Album WHERE Album_isInLibrary = 1")
    suspend fun listAlbums(): List<Album>

    @Query(
        """
        SELECT Album_albumArt_uri FROM Album WHERE Album_albumArt_uri IS NOT NULL
        UNION
        SELECT Album_albumArt_thumbnailUri FROM Album WHERE Album_albumArt_thumbnailUri IS NOT NULL
        """
    )
    suspend fun listImageUris(): List<Uri>

    @Query(
        """
        SELECT DISTINCT t.*, a.* FROM Track t LEFT JOIN Album a ON Track_albumId = Album_albumId
        WHERE Track_albumId IN (:albumIds)
        ORDER BY Track_albumId, Track_discNumber, Track_albumPosition
        """
    )
    suspend fun listTrackPojos(albumIds: List<UUID>): List<TrackPojo>

    @Query("SELECT * FROM Track WHERE Track_albumId = :albumId ORDER BY Track_discNumber, Track_albumPosition")
    suspend fun listTracks(albumId: UUID): List<Track>

    fun searchAlbums(query: String): Flow<List<AlbumPojo>> {
        val terms = query.trim().split(Regex("\\s+")).filter { it.length > 2 }
            .joinToString(" OR ") { "Album_title LIKE '%$it%' OR Album_artist LIKE '%$it%'" }

        return _searchAlbums(
            SimpleSQLiteQuery(
                """
                SELECT a.*, SUM(Track_metadata_durationMs) AS durationMs, MIN(Track_year) AS minYear,
                    MAX(Track_year) AS maxYear, COUNT(Track_trackId) AS trackCount
                FROM Album a LEFT JOIN Track t ON Album_albumId = Track_albumId
                WHERE ($terms) AND Album_isInLibrary = 1
                GROUP BY Album_albumId
                ORDER BY LOWER(Album_artist), LOWER(Album_title)
                """.trimIndent()
            )
        )
    }

    suspend fun updateAlbums(vararg albums: Album) =
        _updateAlbums(*albums.map { it.copy(isInLibrary = true) }.toTypedArray())
}
