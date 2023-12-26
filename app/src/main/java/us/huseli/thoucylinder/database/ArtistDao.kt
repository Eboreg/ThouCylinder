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
            a1.Album_artist AS name,
            COUNT(DISTINCT Track_trackId) AS trackCount,
            COUNT(DISTINCT a1.Album_albumId) AS albumCount,
            a2.Album_albumArt_uri AS firstAlbumArtUri,
            COALESCE(SpotifyAlbum_fullImage_url, a3.Album_youtubePlaylist_thumbnail_url) AS firstAlbumArtUrl,
            COALESCE(SUM(Track_metadata_durationMs), SUM(Track_youtubeVideo_durationMs), 0) AS totalDurationMs
        FROM Album a1
            LEFT JOIN Track ON Track_albumId = a1.Album_albumId AND Track_isInLibrary = 1
            LEFT JOIN Album a2 ON a2.Album_artist = a1.Album_artist 
                AND a2.Album_albumArt_uri IS NOT NULL 
                AND a2.Album_isInLibrary = 1
            LEFT JOIN Album a3 ON a3.Album_artist = a1.Album_artist
                AND a3.Album_youtubePlaylist_thumbnail_url IS NOT NULL
                AND a3.Album_isInLibrary = 1
            LEFT JOIN SpotifyAlbum ON SpotifyAlbum_albumId = a1.Album_albumId
                AND SpotifyAlbum_fullImage_url IS NOT NULL
        WHERE name IS NOT NULL AND a1.Album_isInLibrary = 1
        GROUP BY name
        ORDER BY LOWER(name)
        """
    )
    fun flowAlbumArtistPojos(): Flow<List<ArtistPojo>>

    @Query(
        """
        SELECT
            Track_artist AS name,
            COUNT(DISTINCT Track_trackId) AS trackCount,
            0 AS albumCount,
            a1.Album_albumArt_uri AS firstAlbumArtUri,
            COALESCE(SpotifyAlbum_fullImage_url, a2.Album_youtubePlaylist_thumbnail_url) AS firstAlbumArtUrl,
            COALESCE(SUM(Track_metadata_durationMs), 0) AS totalDurationMs
        FROM Track
            LEFT JOIN Album a1 ON a1.Album_albumId = Track_albumId 
                AND a1.Album_albumArt_uri IS NOT NULL 
                AND a1.Album_isInLibrary = 1
            LEFT JOIN Album a2 ON a2.Album_artist = a1.Album_artist
                AND a2.Album_youtubePlaylist_thumbnail_url IS NOT NULL
                AND a2.Album_isInLibrary = 1
            LEFT JOIN SpotifyAlbum ON SpotifyAlbum_albumId = a1.Album_albumId
                AND SpotifyAlbum_fullImage_url IS NOT NULL
        WHERE name IS NOT NULL 
            AND Track_isInLibrary = 1
            AND NOT EXISTS(
                SELECT Album_artist FROM Album WHERE Album_isInLibrary = 1 AND LOWER(Album_artist) = LOWER(name)
            )
        GROUP BY name
        ORDER BY LOWER(name)
        """
    )
    fun flowTrackArtistPojos(): Flow<List<ArtistPojo>>
}
