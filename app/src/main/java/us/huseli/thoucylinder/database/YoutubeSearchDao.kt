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
abstract class YoutubeSearchDao {
    @Query("DELETE FROM YoutubeSearchToken")
    protected abstract suspend fun _deleteAllTokens()

    @Query("DELETE FROM YoutubeQueryTrack")
    protected abstract suspend fun _deleteAllQueryTracks()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun _insertTracks(vararg tracks: Track)

    @Query(
        """
        INSERT OR IGNORE INTO YoutubeQueryTrack (YoutubeQueryTrack_query, YoutubeQueryTrack_trackId, YoutubeQueryTrack_position)
        VALUES (:query, :trackId, (SELECT COALESCE(MAX(qt2.YoutubeQueryTrack_position), 0) + 1 FROM YoutubeQueryTrack qt2))
        """
    )
    protected abstract suspend fun _insertQueryTrack(query: String, trackId: UUID)

    @Transaction
    open suspend fun clearCache() {
        _deleteAllTokens()
        _deleteAllQueryTracks()
    }

    @Query("SELECT * FROM YoutubeSearchToken WHERE YoutubeSearchToken_query = :query")
    abstract suspend fun getToken(query: String): YoutubeSearchToken?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrReplaceToken(token: YoutubeSearchToken)

    @Transaction
    open suspend fun insertTracksForQuery(query: String, tracks: List<Track>) {
        if (tracks.isNotEmpty()) {
            _insertTracks(*tracks.map { it.copy(isInLibrary = false) }.toTypedArray())
            tracks.forEach { track -> _insertQueryTrack(query, track.trackId) }
        }
    }

    @Query(
        """
        SELECT Track.* FROM Track
            JOIN YoutubeQueryTrack ON Track_trackId = YoutubeQueryTrack_trackId AND YoutubeQueryTrack_query = :query
        ORDER BY YoutubeQueryTrack_position
        """
    )
    abstract fun pageTracksByQuery(query: String): PagingSource<Int, Track>
}
