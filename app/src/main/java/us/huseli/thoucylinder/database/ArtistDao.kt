package us.huseli.thoucylinder.database

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import us.huseli.thoucylinder.dataclasses.combos.ArtistPojo

@Dao
interface ArtistDao {
    @Query(
        """
        SELECT
            Album_artist AS name,
            COUNT(DISTINCT Track_trackId) AS trackCount,
            COUNT(DISTINCT Album_albumId) AS albumCount,
            group_concat(DISTINCT quote(Album_albumArt_uri)) AS albumArtUris,
            group_concat(DISTINCT quote(Album_youtubePlaylist_thumbnail_url)) AS youtubeFullImageUrls,
            group_concat(DISTINCT quote(SpotifyAlbum_fullImage_url)) AS spotifyFullImageUrls,
            COALESCE(SUM(Track_metadata_durationMs), SUM(Track_youtubeVideo_durationMs), 0) AS totalDurationMs
        FROM Album
            LEFT JOIN Track ON Track_albumId = Album_albumId AND Track_isInLibrary = 1
            LEFT JOIN SpotifyAlbum ON SpotifyAlbum_albumId = Album_albumId
        WHERE name IS NOT NULL AND Album_isInLibrary = 1
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
            group_concat(DISTINCT quote(Album_albumArt_uri)) AS albumArtUris,
            group_concat(DISTINCT quote(Album_youtubePlaylist_thumbnail_url)) AS youtubeFullImageUrls,
            group_concat(DISTINCT quote(SpotifyAlbum_fullImage_url)) AS spotifyFullImageUrls,
            COALESCE(SUM(Track_metadata_durationMs), 0) AS totalDurationMs
        FROM Track
            LEFT JOIN Album ON Album_albumId = Track_albumId AND Album_isInLibrary = 1
            LEFT JOIN SpotifyAlbum ON SpotifyAlbum_albumId = Album_albumId
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
