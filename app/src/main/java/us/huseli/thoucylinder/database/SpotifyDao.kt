@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyTrackAudioFeatures
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyTrackIdPair

@Dao
abstract class SpotifyDao {
    @Query("SELECT Track_spotifyId AS spotifyTrackId, Track_trackId AS trackId FROM Track WHERE Track_spotifyId IN (:spotifyTrackIds)")
    protected abstract suspend fun _listTrackIdPairs(spotifyTrackIds: List<String>): List<SpotifyTrackIdPair>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun _insertAudioFeatures(audioFeatures: List<SpotifyTrackAudioFeatures>)

    @Query(
        """
        SELECT Track_spotifyId FROM Track
        LEFT JOIN SpotifyTrackAudioFeatures ON Track_spotifyId = SpotifyTrackAudioFeatures.spotifyTrackId
        WHERE Track_spotifyId IS NOT NULL AND SpotifyTrackAudioFeatures.spotifyTrackId IS NULL
        GROUP BY Track_spotifyId
        """
    )
    abstract fun flowSpotifyTrackIdsWithoutAudioFeatures(): Flow<List<String>>

    @Transaction
    open suspend fun insertAudioFeatures(audioFeatures: List<SpotifyTrackAudioFeatures>) {
        val idPairs = _listTrackIdPairs(audioFeatures.map { it.spotifyTrackId })

        _insertAudioFeatures(
            audioFeatures.mapNotNull { f ->
                idPairs.find { it.spotifyTrackId == f.spotifyTrackId }?.let { f.copy(trackId = it.trackId) }
            }
        )
    }
}
