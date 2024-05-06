@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import android.database.DatabaseUtils
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
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.AlbumArtist
import us.huseli.thoucylinder.dataclasses.entities.AlbumTag
import us.huseli.thoucylinder.dataclasses.entities.Tag
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.entities.TrackArtist
import us.huseli.thoucylinder.dataclasses.pojos.TagPojo
import us.huseli.thoucylinder.dataclasses.views.TrackCombo
import us.huseli.thoucylinder.enums.AvailabilityFilter
import us.huseli.thoucylinder.enums.SortOrder
import us.huseli.thoucylinder.enums.TrackSortParameter

@Dao
abstract class TrackDao {
    @RawQuery(observedEntities = [Tag::class, AlbumTag::class, Album::class])
    protected abstract fun _flowTagPojos(query: SupportSQLiteQuery): Flow<List<TagPojo>>

    @RawQuery(observedEntities = [Track::class, Album::class, AlbumArtist::class, TrackArtist::class])
    @Transaction
    protected abstract fun _pageTrackCombos(query: SupportSQLiteQuery): PagingSource<Int, TrackCombo>

    /** Public methods ********************************************************/
    @Query("UPDATE Track SET Track_localUri = NULL WHERE Track_trackId IN (:trackIds)")
    abstract suspend fun clearLocalUris(trackIds: Collection<String>)

    @Query("DELETE FROM Track")
    abstract suspend fun clearTracks()

    @Delete
    abstract suspend fun deleteTracks(vararg tracks: Track)

    @Query(
        """
        DELETE FROM Track WHERE Track_isInLibrary = 0
        AND NOT EXISTS(SELECT * FROM QueueTrack WHERE QueueTrack_trackId = Track_trackId)
        AND NOT EXISTS(SELECT * FROM PlaylistTrack WHERE PlaylistTrack_trackId = Track_trackId)
        """
    )
    abstract suspend fun deleteTempTracks()

    @Query("DELETE FROM Track WHERE Track_albumId IN (:albumIds)")
    abstract suspend fun deleteTracksByAlbumId(vararg albumIds: String)

    fun flowTagPojos(availabilityFilter: AvailabilityFilter): Flow<List<TagPojo>> {
        val availabilityQuery = when (availabilityFilter) {
            AvailabilityFilter.ALL -> ""
            AvailabilityFilter.ONLY_PLAYABLE -> "AND (Track_localUri IS NOT NULL OR Track_youtubeVideo_id IS NOT NULL)"
            AvailabilityFilter.ONLY_LOCAL -> "AND Track_localUri IS NOT NULL"
        }

        return _flowTagPojos(
            SimpleSQLiteQuery(
                """
                SELECT Tag_name AS name, COUNT(*) AS itemCount
                FROM Tag JOIN AlbumTag ON Tag_name = AlbumTag_tagName
                JOIN Track ON Track_albumId = AlbumTag_albumId
                WHERE Track_isInLibrary = 1
                $availabilityQuery
                GROUP BY Tag_name
                ORDER BY itemCount DESC, Tag_name ASC
                """.trimIndent()
            )
        )
    }

    @Transaction
    @Query("SELECT * FROM TrackCombo WHERE Track_albumId = :albumId")
    abstract fun flowTrackCombosByAlbumId(albumId: String): Flow<List<TrackCombo>>

    @Query("SELECT Track_albumId FROM Track WHERE Track_trackId = :trackId")
    abstract suspend fun getAlbumIdByTrackId(trackId: String): String?

    @Query("SELECT COUNT(*) FROM Track WHERE Track_isInLibrary = 1")
    abstract suspend fun getLibraryTrackCount(): Int

    @Query("SELECT * FROM Track WHERE Track_trackId = :trackId")
    abstract suspend fun getTrackById(trackId: String): Track?

    @Transaction
    @Query("SELECT * FROM TrackCombo WHERE Track_trackId = :trackId")
    abstract suspend fun getTrackComboById(trackId: String): TrackCombo?

    @Query("SELECT Track_localUri FROM Track WHERE Track_localUri IS NOT NULL")
    abstract suspend fun listLocalUris(): List<String>

    @Query("SELECT * FROM Track WHERE Track_albumId IS NULL")
    abstract suspend fun listNonLocalTracks(): List<Track>

    @Transaction
    @Query(
        """
        SELECT * FROM TrackCombo WHERE Track_isInLibrary = 1 AND Track_trackId NOT IN (:exceptTrackIds)
            AND Track_spotifyId NOT IN (:exceptSpotifyTrackIds)
        ORDER BY RANDOM() LIMIT :limit
        """
    )
    abstract suspend fun listRandomLibraryTrackCombos(
        limit: Int,
        exceptTrackIds: Collection<String> = emptyList(),
        exceptSpotifyTrackIds: Collection<String> = emptyList(),
    ): List<TrackCombo>

    @Transaction
    @Query("SELECT * FROM TrackCombo WHERE Track_albumId = :albumId ORDER BY Track_discNumber, Track_albumPosition")
    abstract suspend fun listTrackCombosByAlbumId(albumId: String): List<TrackCombo>

    @Transaction
    @Query(
        """
        SELECT TrackCombo.* FROM TrackCombo
            LEFT JOIN TrackArtistCredit ON Track_trackId = TrackArtist_trackId
            LEFT JOIN AlbumArtistCredit ON Track_albumId = AlbumArtist_albumId
        WHERE (
            TrackArtist_artistId = :artistId
            OR (TrackArtist_artistId IS NULL AND AlbumArtist_artistId = :artistId)
        ) AND Track_isInLibrary = 1
        GROUP BY Track_trackId
        """
    )
    abstract suspend fun listTrackCombosByArtistId(artistId: String): List<TrackCombo>

    @Transaction
    @Query("SELECT * FROM TrackCombo WHERE Track_trackId IN (:trackIds)")
    abstract suspend fun listTrackCombosById(vararg trackIds: String): List<TrackCombo>

    @Query("SELECT Track_trackId FROM Track WHERE Track_albumId IN (:albumIds)")
    abstract suspend fun listTrackIdsByAlbumId(vararg albumIds: String): List<String>

    @Query(
        """
        SELECT Track_trackId FROM Track
            LEFT JOIN TrackArtistCredit ON Track_trackId = TrackArtist_trackId
            LEFT JOIN AlbumArtistCredit ON Track_albumId = AlbumArtist_albumId
        WHERE (
            TrackArtist_artistId = :artistId
            OR (TrackArtist_artistId IS NULL AND AlbumArtist_artistId = :artistId)
        ) AND Track_isInLibrary = 1
        GROUP BY Track_trackId
        """
    )
    abstract suspend fun listTrackIdsByArtistId(artistId: String): List<String>

    @Query("SELECT * FROM Track WHERE Track_albumId = :albumId")
    abstract suspend fun listTracksByAlbumId(albumId: String): List<Track>

    @Query("SELECT * FROM Track WHERE Track_trackId IN (:trackIds) ORDER BY Track_discNumber, Track_albumPosition")
    abstract suspend fun listTracksById(vararg trackIds: String): List<Track>

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
                SELECT TrackCombo.*, GROUP_CONCAT(TrackArtist_name, '/') AS trackArtist
                FROM TrackCombo
                    LEFT JOIN TrackArtistCredit ON Track_trackId = TrackArtist_trackId
                    $tagJoin
                WHERE Track_isInLibrary = 1 AND (Album_isHidden IS NULL OR Album_isHidden != 1) $availabilityQuery 
                GROUP BY Track_trackId
                ${searchQuery?.let { "HAVING $it" } ?: ""} 
                ORDER BY ${sortParameter.sql(sortOrder)}
                """.trimIndent()
            )
        )
    }

    @Transaction
    @Query(
        """
        SELECT TrackCombo.* FROM TrackCombo 
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
    abstract fun pageTrackCombosByArtist(artistId: String): PagingSource<Int, TrackCombo>

    @Transaction
    open suspend fun setAlbumTracks(albumId: String, tracks: Collection<Track>) {
        deleteTracksByAlbumId(albumId)
        if (tracks.isNotEmpty()) upsertTracks(*tracks.toTypedArray())
    }

    @Query("UPDATE Track SET Track_isInLibrary = :isInLibrary WHERE Track_trackId IN (:trackIds)")
    abstract suspend fun setIsInLibrary(isInLibrary: Boolean, vararg trackIds: String)

    @Query("UPDATE Track SET Track_isInLibrary = :isInLibrary WHERE Track_albumId IN (:albumIds)")
    abstract suspend fun setIsInLibraryByAlbumId(isInLibrary: Boolean, vararg albumIds: String)

    @Query("UPDATE Track SET Track_amplitudes = :amplitudes WHERE Track_trackId = :trackId")
    abstract suspend fun setTrackAmplitudes(trackId: String, amplitudes: String)

    @Query("UPDATE Track SET Track_spotifyId = :spotifyId WHERE Track_trackId = :trackId")
    abstract suspend fun setTrackSpotifyId(trackId: String, spotifyId: String)

    @Update
    abstract suspend fun updateTracks(vararg tracks: Track)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertTracks(vararg tracks: Track)
}
