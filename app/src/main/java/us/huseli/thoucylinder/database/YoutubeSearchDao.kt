@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.entities.YoutubeSearchToken
import java.util.UUID

@Dao
interface YoutubeSearchDao {
    @Query("DELETE FROM Track WHERE Track_isInLibrary = 0")
    suspend fun _deleteAllTracks()

    @Query("DELETE FROM YoutubeSearchToken")
    suspend fun _deleteAllTokens()

    @Query("DELETE FROM YoutubeQueryTrack")
    suspend fun _deleteAllQueryTracks()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun _insertTracks(vararg tracks: Track)

    @Query(
        """
        INSERT OR IGNORE INTO YoutubeQueryTrack (YoutubeQueryTrack_query, YoutubeQueryTrack_trackId, YoutubeQueryTrack_position)
        VALUES (:query, :trackId, (SELECT COALESCE(MAX(qt2.YoutubeQueryTrack_position), 0) + 1 FROM YoutubeQueryTrack qt2))
        """
    )
    suspend fun _insertQueryTrack(query: String, trackId: UUID)

    @Transaction
    suspend fun clearCache() {
        _deleteAllTokens()
        _deleteAllQueryTracks()
        _deleteAllTracks()
    }

    @Query("SELECT * FROM YoutubeSearchToken WHERE YoutubeSearchToken_query = :query")
    suspend fun getToken(query: String): YoutubeSearchToken?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceToken(token: YoutubeSearchToken)

    @Transaction
    suspend fun insertTracksForQuery(query: String, tracks: List<Track>) {
        _insertTracks(*tracks.map { it.copy(isInLibrary = false) }.toTypedArray())
        tracks.forEach { track -> _insertQueryTrack(query, track.trackId) }
    }

    @Query(
        """
        SELECT t.* FROM Track t
        JOIN YoutubeQueryTrack y ON Track_trackId = YoutubeQueryTrack_trackId
        AND YoutubeQueryTrack_query = :query
        ORDER BY YoutubeQueryTrack_position
        """
    )
    fun pageTracksByQuery(query: String): PagingSource<Int, Track>
}
