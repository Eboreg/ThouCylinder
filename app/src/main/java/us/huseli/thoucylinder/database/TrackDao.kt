@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import android.database.DatabaseUtils
import android.net.Uri
import androidx.paging.PagingSource
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
import us.huseli.thoucylinder.AvailabilityFilter
import us.huseli.thoucylinder.SortOrder
import us.huseli.thoucylinder.TrackSortParameter
import us.huseli.thoucylinder.dataclasses.combos.TrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.AlbumTag
import us.huseli.thoucylinder.dataclasses.entities.Tag
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.TagPojo
import us.huseli.thoucylinder.dataclasses.views.AlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit
import java.util.UUID

@Dao
interface TrackDao {
    @RawQuery(observedEntities = [Tag::class, AlbumTag::class, Album::class])
    fun _flowTagPojos(query: SupportSQLiteQuery): Flow<List<TagPojo>>

    @RawQuery(
        observedEntities = [
            Track::class,
            Album::class,
            AlbumArtistCredit::class,
            TrackArtistCredit::class,
        ]
    )
    @Transaction
    fun _pageTrackCombos(query: SupportSQLiteQuery): PagingSource<Int, TrackCombo>

    /** Public methods ********************************************************/
    @Query("UPDATE Track SET Track_localUri = NULL WHERE Track_trackId IN (:trackIds)")
    suspend fun clearLocalUris(trackIds: Collection<UUID>)

    @Query("DELETE FROM Track")
    suspend fun clearTracks()

    @Delete
    suspend fun deleteTracks(vararg tracks: Track)

    @Query("DELETE FROM Track WHERE Track_isInLibrary = 0")
    suspend fun deleteTempTracks()

    @Query("DELETE FROM Track WHERE Track_albumId IN (:albumIds)")
    suspend fun deleteTracksByAlbumId(vararg albumIds: UUID)

    @Query(
        """
        SELECT Tag_name AS name, COUNT(*) AS itemCount
        FROM Tag JOIN AlbumTag ON Tag_name = AlbumTag_tagName JOIN Track ON Track_albumId = AlbumTag_albumId
        GROUP BY Tag_name
        ORDER BY itemCount DESC, Tag_name ASC
        """
    )
    fun flowTagPojos(): Flow<List<TagPojo>>

    fun flowTagPojos(availabilityFilter: AvailabilityFilter): Flow<List<TagPojo>> {
        val availabilityQuery = when (availabilityFilter) {
            AvailabilityFilter.ALL -> ""
            AvailabilityFilter.ONLY_PLAYABLE -> "WHERE Album_isLocal = 1 OR Album_youtubePlaylist_id IS NOT NULL"
            AvailabilityFilter.ONLY_LOCAL -> "WHERE Album_isLocal = 1"
        }

        return _flowTagPojos(
            SimpleSQLiteQuery(
                """
                SELECT Tag_name AS name, COUNT(*) AS itemCount
                FROM Tag JOIN AlbumTag ON Tag_name = AlbumTag_tagName
                JOIN Album ON Album_albumId = AlbumTag_albumId
                JOIN Track ON Track_albumId = Album_albumId
                $availabilityQuery
                GROUP BY Tag_name
                ORDER BY itemCount DESC, Tag_name ASC
                """.trimIndent()
            )
        )
    }

    @Query("SELECT * FROM Track WHERE Track_isInLibrary = 1")
    suspend fun listLibraryTracks(): List<Track>

    @Query("SELECT Track_localUri FROM Track WHERE Track_localUri IS NOT NULL")
    suspend fun listLocalUris(): List<Uri>

    @Transaction
    @Query("SELECT * FROM TrackCombo WHERE Track_trackId IN (:trackIds)")
    suspend fun listTrackCombosById(vararg trackIds: UUID): List<TrackCombo>

    @Query("SELECT * FROM Track WHERE Track_trackId IN (:trackIds) ORDER BY Track_discNumber, Track_albumPosition")
    suspend fun listTracksById(vararg trackIds: UUID): List<Track>

    fun pageTrackCombos(
        sortParameter: TrackSortParameter,
        sortOrder: SortOrder,
        searchTerm: String,
        tagNames: List<String>,
        availabilityFilter: AvailabilityFilter,
    ): PagingSource<Int, TrackCombo> {
        val searchQuery = searchTerm
            .lowercase()
            .split(Regex(" +"))
            .filter { it.isNotEmpty() }
            .takeIf { it.isNotEmpty() }
            ?.map { DatabaseUtils.sqlEscapeString("%$it%") }
            ?.joinToString(" AND ") { term ->
                "(LOWER(Track_title) LIKE $term OR LOWER(Album_title) LIKE $term OR " +
                    "LOWER(trackArtist) LIKE $term OR LOWER(albumArtist) LIKE $term OR " +
                    "Album_year LIKE $term OR Track_year LIKE $term)"
            }
        val tagJoin =
            if (tagNames.isNotEmpty()) "JOIN AlbumTag ON Track_albumId = AlbumTag_albumId AND AlbumTag_tagName IN " +
                "(${tagNames.joinToString(", ") { "\"$it\"" }})"
            else ""
        val availabilityQuery = when (availabilityFilter) {
            AvailabilityFilter.ALL -> ""
            AvailabilityFilter.ONLY_PLAYABLE ->
                "AND (Track_localUri IS NOT NULL OR Track_youtubeVideo_id IS NOT NULL)"
            AvailabilityFilter.ONLY_LOCAL -> "AND Track_localUri IS NOT NULL"
        }

        return _pageTrackCombos(
            SimpleSQLiteQuery(
                """
                SELECT *, GROUP_CONCAT(TrackArtist_name, '/') AS trackArtist
                FROM TrackCombo
                    LEFT JOIN TrackArtistCredit ON Track_trackId = TrackArtist_trackId
                    $tagJoin
                WHERE Track_isInLibrary = 1 AND (Album_isHidden IS NULL OR Album_isHidden != 1) $availabilityQuery 
                GROUP BY Track_trackId
                ${searchQuery?.let { "HAVING $it" } ?: ""} 
                ORDER BY ${sortParameter.sqlColumn} ${sortOrder.sql}
                """.trimIndent()
            )
        )
    }

    @Transaction
    @Query(
        """
        SELECT DISTINCT Track.*, Album.*, GROUP_CONCAT(AlbumArtist_name, '/') AS albumArtist
        FROM Track
            LEFT JOIN Album ON Track_albumId = Album_albumId
            LEFT JOIN AlbumArtistCredit ON Track_albumId = AlbumArtist_albumId
            LEFT JOIN TrackArtistCredit ON Track_trackId = TrackArtist_trackId
        WHERE (
            TrackArtist_artistId = :artistId
            OR (TrackArtist_artistId IS NULL AND AlbumArtist_artistId = :artistId)
        ) AND Track_isInLibrary = 1
        GROUP BY Track_trackId                    
        ORDER BY LOWER(Track_title)
        """
    )
    fun pageTrackCombosByArtist(artistId: UUID): PagingSource<Int, TrackCombo>

    @Transaction
    suspend fun setAlbumTracks(albumId: UUID, tracks: Collection<Track>) {
        deleteTracksByAlbumId(albumId)
        if (tracks.isNotEmpty()) upsertTracks(*tracks.toTypedArray())
    }

    @Query("UPDATE Track SET Track_isInLibrary = :isInLibrary WHERE Track_albumId = :albumId")
    suspend fun setIsInLibraryByAlbumId(albumId: UUID, isInLibrary: Boolean)

    @Query("UPDATE Track SET Track_isInLibrary = :isInLibrary WHERE Track_trackId IN (:trackIds)")
    suspend fun setIsInLibrary(trackIds: Collection<UUID>, isInLibrary: Boolean)

    @Update
    suspend fun updateTracks(vararg tracks: Track)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTracks(vararg tracks: Track)
}
