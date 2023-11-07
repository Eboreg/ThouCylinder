package us.huseli.thoucylinder.database

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import us.huseli.thoucylinder.dataclasses.pojos.ArtistPojo

@Dao
interface ArtistDao {
    @Query(
        """
        SELECT
            COALESCE(Track_artist, a.Album_artist) AS name,
            COUNT(DISTINCT Track_trackId) AS trackCount,
            (
                SELECT COUNT(*) FROM Album a2
                WHERE LOWER(a2.Album_artist) = LOWER(COALESCE(Track_artist, a.Album_artist)) AND a2.Album_isInLibrary = 1
            ) AS albumCount,
            a3.Album_albumArt_uri AS firstAlbumArt,
            a4.Album_albumArt_thumbnailUri AS firstAlbumArtThumbnail,
            COALESCE(SUM(Track_metadata_durationMs), 0) AS totalDurationMs
        FROM Track t
            LEFT JOIN Album a ON Track_albumId = a.Album_albumId AND a.Album_isInLibrary = 1
            LEFT JOIN Album a3 ON a3.Album_albumId = Track_albumId 
                AND a3.Album_albumArt_uri IS NOT NULL 
                AND a3.Album_isInLibrary = 1
            LEFT JOIN Album a4 ON a4.Album_albumId = Track_albumId 
                AND a4.Album_albumArt_thumbnailUri IS NOT NULL 
                AND a4.Album_isInLibrary = 1
        WHERE name IS NOT NULL AND Track_isInLibrary = 1
        GROUP BY name
        ORDER BY LOWER(name)
        """
    )
    fun flowArtistPojos(): Flow<List<ArtistPojo>>
}
