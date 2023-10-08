@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import us.huseli.thoucylinder.dataclasses.ArtistPojo

@Dao
interface ArtistDao {
    @Query(
        """
        SELECT
            COALESCE(t.artist, a.artist) AS name,
            COUNT(DISTINCT t.trackId) AS trackCount,
            (SELECT COUNT(*) FROM Album a2 WHERE a2.artist = COALESCE(t.artist, a.artist) AND a2.isInLibrary = 1) AS albumCount,
            a3.albumArtlocalFile AS firstAlbumArt,
            COALESCE(SUM(t.metadatadurationMs), 0) AS totalDurationMs
        FROM Track t
            LEFT JOIN Album a ON t.albumId = a.albumId AND a.isInLibrary = 1
            LEFT JOIN Album a3 ON a3.albumId = t.albumId AND a3.albumArtlocalFile IS NOT NULL AND a3.isInLibrary = 1
        WHERE name IS NOT NULL AND t.isInLibrary = 1
        GROUP BY name
        ORDER BY LOWER(name)
        """
    )
    fun flowArtistPojos(): Flow<List<ArtistPojo>>
}
