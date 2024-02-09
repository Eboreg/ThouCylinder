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
import us.huseli.thoucylinder.AlbumSortParameter
import us.huseli.thoucylinder.SortOrder
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.AlbumGenre
import us.huseli.thoucylinder.dataclasses.entities.AlbumStyle
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbum
import us.huseli.thoucylinder.dataclasses.entities.Style
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.combos.AlbumCombo
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.combos.TrackCombo
import java.util.UUID

@Dao
interface AlbumDao {
    /** Pseudo-private methods ************************************************/
    @RawQuery(
        observedEntities = [
            Album::class,
            Track::class,
            Genre::class,
            Style::class,
            SpotifyAlbum::class,
        ],
    )
    fun _flowAlbumCombos(query: SupportSQLiteQuery): Flow<List<AlbumCombo>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun _insertAlbumGenres(vararg albumGenres: AlbumGenre)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun _insertAlbumStyles(vararg albumStyles: AlbumStyle)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun _insertGenres(vararg genres: Genre)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun _insertStyles(vararg styles: Style)

    /** Public methods ********************************************************/
    @Query("SELECT EXISTS(SELECT Album_albumId FROM Album WHERE Album_albumId = :albumId)")
    suspend fun albumExists(albumId: UUID): Boolean

    @Query("UPDATE Album SET Album_albumArt_uri = NULL, Album_albumArt_thumbnailUri = NULL WHERE Album_albumId = :albumId")
    suspend fun clearAlbumArt(albumId: UUID)

    @Query("DELETE FROM AlbumGenre WHERE AlbumGenre_albumId IN (:albumIds)")
    suspend fun clearAlbumGenres(albumIds: Collection<UUID>)

    @Query("DELETE FROM AlbumStyle WHERE AlbumStyle_albumId IN (:albumIds)")
    suspend fun clearAlbumStyles(albumIds: Collection<UUID>)

    @Delete
    suspend fun deleteAlbums(vararg albums: Album)

    @Query("DELETE FROM Album WHERE Album_isInLibrary = 0")
    suspend fun deleteTempAlbums()

    fun flowAlbumCombosByArtist(artist: String) = flowAlbumCombos(
        sortParameter = AlbumSortParameter.TITLE,
        sortOrder = SortOrder.ASCENDING,
        artist = artist,
    )

    fun flowAlbumCombos(
        sortParameter: AlbumSortParameter,
        sortOrder: SortOrder,
        artist: String? = null,
        searchTerm: String? = null,
    ): Flow<List<AlbumCombo>> {
        val searchQuery = searchTerm
            ?.lowercase()
            ?.split(Regex(" +"))
            ?.takeIf { it.isNotEmpty() }
            ?.map { DatabaseUtils.sqlEscapeString("%$it%") }
            ?.joinToString(" AND ") { term ->
                "(LOWER(Album_artist) LIKE $term OR LOWER(Album_title) LIKE $term OR Album_year LIKE $term)"
            }

        return _flowAlbumCombos(
            SimpleSQLiteQuery(
                """
                SELECT Album.*, 
                    SUM(COALESCE(Track_metadata_durationMs, Track_youtubeVideo_durationMs, Track_youtubeVideo_metadata_durationMs)) AS durationMs,
                    MIN(Track_year) AS minYear, MAX(Track_year) AS maxYear, COUNT(Track_trackId) AS trackCount,
                    EXISTS(SELECT * FROM Track WHERE Track_albumId = Album_albumId AND Track_localUri IS NOT NULL) AND 
                    EXISTS(SELECT * FROM Track WHERE Track_albumId = Album_albumId AND Track_localUri IS NULL) AS isPartiallyDownloaded
                FROM Album LEFT JOIN Track ON Album_albumId = Track_albumId 
                WHERE Album_isInLibrary = 1 AND Album_isDeleted = 0 AND Album_isHidden = 0
                    ${artist?.let { "AND LOWER(Album_artist) = LOWER(\"$it\")" } ?: ""}
                    ${searchQuery?.let { "AND $it" } ?: ""}                                
                GROUP BY Album_albumId
                ORDER BY ${sortParameter.sqlColumn} ${if (sortOrder == SortOrder.ASCENDING) "ASC" else "DESC"}
                """.trimIndent()
            )
        )
    }

    @Query("SELECT * FROM Album WHERE Album_albumId = :albumId")
    @Transaction
    fun flowAlbumWithTracks(albumId: UUID): Flow<AlbumWithTracksCombo?>

    @Query("SELECT * FROM Genre ORDER BY Genre_genreName")
    fun flowGenres(): Flow<List<Genre>>

    @Query("SELECT * FROM Album WHERE Album_albumId = :albumId")
    suspend fun getAlbum(albumId: UUID): Album?

    @Query("SELECT * FROM Album WHERE Album_albumId = :albumId")
    @Transaction
    suspend fun getAlbumWithTracks(albumId: UUID): AlbumWithTracksCombo?

    @Transaction
    suspend fun insertAlbumGenres(albumGenres: Collection<AlbumGenre>) {
        _insertGenres(*albumGenres.map { Genre(it.genreName) }.toTypedArray())
        _insertAlbumGenres(*albumGenres.toTypedArray())
    }

    @Transaction
    suspend fun insertAlbumStyles(albumStyles: Collection<AlbumStyle>) {
        _insertStyles(*albumStyles.map { Style(it.styleName) }.toTypedArray())
        _insertAlbumStyles(*albumStyles.toTypedArray())
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbums(vararg albums: Album)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGenres(vararg genres: Genre)

    @Query("SELECT * FROM Album WHERE Album_isInLibrary = 1")
    suspend fun listAlbums(): List<Album>

    @Query("SELECT * FROM Album WHERE Album_isDeleted = 1")
    suspend fun listDeletionMarkedAlbums(): List<Album>

    @Query("SELECT Album_albumId FROM Album WHERE Album_albumId IN (:albumIds)")
    suspend fun listExistingAlbumIds(albumIds: Collection<UUID>): List<UUID>

    @Query("SELECT * FROM Genre")
    suspend fun listGenres(): List<Genre>

    @Query("SELECT Album_musicBrainzReleaseId FROM Album WHERE Album_musicBrainzReleaseId IS NOT NULL")
    suspend fun listMusicBrainzReleaseIds(): List<String>

    @Query(
        """
        SELECT DISTINCT Track.*, Album.*, SpotifyTrack.*
        FROM Track
            LEFT JOIN Album ON Track_albumId = Album_albumId
            LEFT JOIN SpotifyTrack ON Track_trackId = SpotifyTrack_trackId
        WHERE Track_albumId IN (:albumIds)
        ORDER BY Track_albumId, Track_discNumber, Track_albumPosition
        """
    )
    suspend fun listTrackCombos(albumIds: List<UUID>): List<TrackCombo>

    @Query("SELECT * FROM Track WHERE Track_albumId = :albumId ORDER BY Track_discNumber, Track_albumPosition")
    suspend fun listTracks(albumId: UUID): List<Track>

    @Query("UPDATE Album SET Album_isDeleted = :isDeleted WHERE Album_albumId = :albumId")
    suspend fun setIsDeleted(albumId: UUID, isDeleted: Boolean)

    @Query("UPDATE Album SET Album_isHidden = :value WHERE Album_albumId = :albumId")
    suspend fun setIsHidden(albumId: UUID, value: Boolean)

    @Query("UPDATE Album SET Album_isInLibrary = :isInLibrary WHERE Album_albumId IN (:albumIds)")
    suspend fun setIsInLibrary(albumIds: Collection<UUID>, isInLibrary: Boolean)

    @Query("UPDATE Album SET Album_isLocal = :isLocal WHERE Album_albumId IN (:albumIds)")
    suspend fun setIsLocal(albumIds: Collection<UUID>, isLocal: Boolean)

    @Query("UPDATE Album SET Album_albumArt_uri = :uri, Album_albumArt_thumbnailUri = :thumbnailUri WHERE Album_albumId = :albumId")
    suspend fun updateAlbumArt(albumId: UUID, uri: Uri, thumbnailUri: Uri)

    @Update
    suspend fun updateAlbums(vararg albums: Album)
}
