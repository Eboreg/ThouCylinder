@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import android.database.DatabaseUtils
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
import us.huseli.thoucylinder.AlbumSortParameter
import us.huseli.thoucylinder.SortOrder
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
    /** Pseudo-private methods ************************************************/
    @Query("SELECT * FROM AlbumGenre")
    fun _flowAlbumGenres(): Flow<List<AlbumGenre>>

    @Query("SELECT * FROM AlbumStyle")
    fun _flowAlbumStyles(): Flow<List<AlbumStyle>>

    fun _flowAlbumPojos(
        sortParameter: AlbumSortParameter,
        sortOrder: SortOrder,
        artist: String? = null,
        searchTerm: String? = null,
    ): Flow<List<AlbumPojo>> {
        val searchQuery = searchTerm
            ?.lowercase()
            ?.split(Regex(" +"))
            ?.takeIf { it.isNotEmpty() }
            ?.map { DatabaseUtils.sqlEscapeString("%$it%") }
            ?.joinToString(" AND ") { term ->
                "(LOWER(Album_artist) LIKE $term OR LOWER(Album_title) LIKE $term OR Album_year LIKE $term)"
            }

        return _flowAlbumPojos(
            SimpleSQLiteQuery(
                """
                SELECT Album.*, 
                    SUM(COALESCE(Track_metadata_durationMs, Track_youtubeVideo_durationMs, Track_youtubeVideo_metadata_durationMs)) AS durationMs,
                    MIN(Track_year) AS minYear, MAX(Track_year) AS maxYear, COUNT(Track_trackId) AS trackCount,
                    EXISTS(SELECT * FROM Track WHERE Track_albumId = Album_albumId AND Track_mediaStoreData_uri IS NOT NULL) AND 
                    EXISTS(SELECT * FROM Track WHERE Track_albumId = Album_albumId AND Track_mediaStoreData_uri IS NULL) AS isPartiallyDownloaded
                FROM Album LEFT JOIN Track ON Album_albumId = Track_albumId 
                WHERE Album_isInLibrary = 1
                    ${artist?.let { "AND LOWER(Album_artist) = LOWER(\"$it\")" } ?: ""}
                    ${searchQuery?.let { "AND $it" } ?: ""}                                
                GROUP BY Album_albumId
                ORDER BY ${sortParameter.sqlColumn} ${if (sortOrder == SortOrder.ASCENDING) "ASC" else "DESC"}
                """.trimIndent()
            )
        )
    }

    @RawQuery(observedEntities = [Album::class, Track::class])
    fun _flowAlbumPojos(query: SupportSQLiteQuery): Flow<List<AlbumPojo>>

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
    fun _searchAlbumPojos(query: SupportSQLiteQuery): Flow<List<AlbumPojo>>

    /** Public methods ********************************************************/
    @Query("SELECT EXISTS(SELECT Album_albumId FROM Album WHERE Album_albumId = :albumId)")
    suspend fun albumExists(albumId: UUID): Boolean

    @Query("DELETE FROM AlbumGenre WHERE AlbumGenre_albumId = :albumId")
    suspend fun clearAlbumGenres(albumId: UUID)

    @Query("DELETE FROM AlbumStyle WHERE AlbumStyle_albumId = :albumId")
    suspend fun clearAlbumStyles(albumId: UUID)

    @Delete
    suspend fun deleteAlbums(vararg albums: Album)

    @Query("DELETE FROM Album WHERE Album_isInLibrary = 0")
    suspend fun deleteTempAlbums()

    fun flowAlbumPojos(
        sortParameter: AlbumSortParameter,
        sortOrder: SortOrder,
        searchTerm: String? = null,
        artist: String? = null,
    ): Flow<List<AlbumPojo>> = combine(
        _flowAlbumPojos(sortParameter = sortParameter, sortOrder = sortOrder, artist = artist, searchTerm = searchTerm),
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

    fun flowAlbumPojosByArtist(artist: String) = flowAlbumPojos(
        sortParameter = AlbumSortParameter.TITLE,
        sortOrder = SortOrder.ASCENDING,
        artist = artist,
    )

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbums(vararg albums: Album)

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

    @Update
    suspend fun updateAlbums(vararg albums: Album)
}
