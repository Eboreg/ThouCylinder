@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import android.database.DatabaseUtils
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import us.huseli.thoucylinder.dataclasses.album.Album
import us.huseli.thoucylinder.dataclasses.artist.AlbumArtist
import us.huseli.thoucylinder.dataclasses.artist.TrackArtist
import us.huseli.thoucylinder.dataclasses.tag.AlbumTag
import us.huseli.thoucylinder.dataclasses.tag.Tag
import us.huseli.thoucylinder.dataclasses.tag.TagPojo
import us.huseli.thoucylinder.dataclasses.track.Track
import us.huseli.thoucylinder.dataclasses.track.TrackCombo
import us.huseli.thoucylinder.enums.AvailabilityFilter
import us.huseli.thoucylinder.enums.SortOrder
import us.huseli.thoucylinder.enums.TrackSortParameter

@Dao
abstract class TrackDao {
    @Query("DELETE FROM Track WHERE Track_albumId = :albumId AND Track_trackId NOT IN (:except)")
    protected abstract suspend fun _deleteTracksByAlbumId(albumId: String, except: Collection<String>)

    @RawQuery(observedEntities = [Tag::class, AlbumTag::class, Album::class])
    protected abstract fun _flowTagPojos(query: SupportSQLiteQuery): Flow<List<TagPojo>>

    @Transaction
    @RawQuery(observedEntities = [Track::class, Album::class, AlbumArtist::class, TrackArtist::class])
    protected abstract fun _flowTrackCombos(query: SupportSQLiteQuery): Flow<List<TrackCombo>>


    /** PUBLIC METHODS ************************************************************************************************/

    @Query("UPDATE Track SET Track_localUri = NULL WHERE Track_trackId IN (:trackIds)")
    abstract suspend fun clearLocalUris(trackIds: Collection<String>)

    @Query("DELETE FROM Track")
    abstract suspend fun clearTracks()

    @Query(
        """
        DELETE FROM Track WHERE Track_isInLibrary = 0
        AND NOT EXISTS(SELECT * FROM QueueTrack WHERE QueueTrack_trackId = Track_trackId)
        AND NOT EXISTS(SELECT * FROM PlaylistTrack WHERE PlaylistTrack_trackId = Track_trackId)
        AND NOT EXISTS(SELECT * FROM QueueTrackCombo WHERE QueueTrackCombo.Album_albumId = Track.Track_albumId)
        """
    )
    abstract suspend fun deleteTempTracks()

    @Query("DELETE FROM Track WHERE Track_trackId IN (:trackIds)")
    abstract suspend fun deleteTracksById(vararg trackIds: String)

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

    fun flowTrackCombos(
        sortParameter: TrackSortParameter,
        sortOrder: SortOrder,
        searchTerm: String,
        tagNames: List<String>,
        availabilityFilter: AvailabilityFilter,
    ): Flow<List<TrackCombo>> {
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

        return _flowTrackCombos(
            SimpleSQLiteQuery(
                """
                SELECT TrackCombo.*,
                    GROUP_CONCAT(TrackArtist_name, '/') AS trackArtist,
                    GROUP_CONCAT(AlbumArtist_name, '/') AS albumArtist
                FROM TrackCombo
                    LEFT JOIN TrackArtistCredit ON Track_trackId = TrackArtist_trackId
                    LEFT JOIN AlbumArtistCredit ON Album_albumId = AlbumArtist_albumId
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
    @Query("SELECT * FROM TrackCombo WHERE Track_albumId = :albumId")
    abstract fun flowTrackCombosByAlbumId(albumId: String): Flow<List<TrackCombo>>

    @Transaction
    @Query(
        """
        SELECT TrackCombo.* FROM TrackCombo 
            LEFT JOIN AlbumArtistCredit ON Track_albumId = AlbumArtist_albumId
            LEFT JOIN TrackArtistCredit ON Track_trackId = TrackArtist_trackId
        WHERE 
            TrackArtist_artistId = :artistId
            OR (TrackArtist_artistId IS NULL AND AlbumArtist_artistId = :artistId)
        GROUP BY Track_trackId                    
        ORDER BY LOWER(Track_title)
        """
    )
    abstract fun flowTrackCombosByArtist(artistId: String): Flow<List<TrackCombo>>

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
    abstract suspend fun listNonAlbumTracks(): List<Track>

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
        exceptTrackIds: List<String>,
        exceptSpotifyTrackIds: List<String>,
    ): List<TrackCombo>

    @Transaction
    @Query("SELECT * FROM TrackCombo WHERE Track_albumId IN (:albumIds) ORDER BY Track_albumId, Track_discNumber, Track_albumPosition")
    abstract suspend fun listTrackCombosByAlbumId(vararg albumIds: String): List<TrackCombo>

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
    @Query("SELECT * FROM TrackCombo WHERE Track_trackId IN (:trackIds) ORDER BY Track_albumId, Track_discNumber, Track_albumPosition")
    abstract suspend fun listTrackCombosById(vararg trackIds: String): List<TrackCombo>

    @Query("SELECT Track_trackId FROM Track WHERE Track_isInLibrary = 1")
    abstract suspend fun listTrackIds(): List<String>

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

    @Transaction
    open suspend fun setAlbumTracks(albumId: String, tracks: Collection<Track>) {
        _deleteTracksByAlbumId(albumId = albumId, except = tracks.map { it.trackId })
        if (tracks.isNotEmpty()) upsertTracks(*tracks.toTypedArray())
    }

    @Query("UPDATE Track SET Track_isInLibrary = :isInLibrary WHERE Track_trackId IN (:trackIds)")
    abstract suspend fun setIsInLibrary(isInLibrary: Boolean, vararg trackIds: String)

    @Query("UPDATE Track SET Track_isInLibrary = :isInLibrary WHERE Track_albumId IN (:albumIds)")
    abstract suspend fun setIsInLibraryByAlbumId(isInLibrary: Boolean, vararg albumIds: String)

    @Query("UPDATE Track SET Track_localPlayCount = :localPlayCount, Track_lastFmPlayCount = :lastFmPlayCount WHERE Track_trackId = :trackId")
    abstract suspend fun setPlayCounts(trackId: String, localPlayCount: Int, lastFmPlayCount: Int?)

    @Query("UPDATE Track SET Track_amplitudes = :amplitudes WHERE Track_trackId = :trackId")
    abstract suspend fun setTrackAmplitudes(trackId: String, amplitudes: String)

    @Query("UPDATE Track SET Track_spotifyId = :spotifyId WHERE Track_trackId = :trackId")
    abstract suspend fun setTrackSpotifyId(trackId: String, spotifyId: String)

    @Transaction
    @Upsert
    abstract suspend fun upsertTracks(vararg tracks: Track)
}
