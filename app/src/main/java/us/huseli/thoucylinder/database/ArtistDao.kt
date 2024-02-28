@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import us.huseli.thoucylinder.dataclasses.combos.ArtistCombo
import us.huseli.thoucylinder.dataclasses.entities.AlbumArtist
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.entities.TrackArtist
import us.huseli.thoucylinder.dataclasses.pojos.TopLocalSpotifyArtistPojo
import java.util.UUID

@Dao
interface ArtistDao {
    @Query("SELECT * FROM Artist WHERE Artist_name = :name LIMIT 1")
    suspend fun _getArtistByName(name: String): Artist?

    @Insert
    suspend fun _insertArtists(vararg artists: Artist)

    @Query("DELETE FROM AlbumArtist WHERE AlbumArtist_albumId IN(:albumIds)")
    suspend fun clearAlbumArtists(vararg albumIds: UUID)

    @Query("DELETE FROM Artist")
    suspend fun clearArtists()

    @Query("DELETE FROM TrackArtist WHERE TrackArtist_trackId IN(:trackIds)")
    suspend fun clearTrackArtists(vararg trackIds: UUID)

    @Query(
        """
        DELETE FROM Artist 
        WHERE NOT EXISTS(SELECT * FROM AlbumArtist WHERE AlbumArtist_artistId = Artist_id)
            AND NOT EXISTS(SELECT * FROM TrackArtist WHERE TrackArtist_artistId = Artist_id)
        """
    )
    suspend fun deleteOrphans()

    @Query(
        """
        SELECT
            Artist.*,
            COUNT(DISTINCT Track_trackId) AS trackCount,
            COUNT(DISTINCT Album_albumId) AS albumCount,
            group_concat(DISTINCT quote(Album_albumArt_uri)) AS albumArtUris,
            group_concat(DISTINCT quote(Album_youtubePlaylist_thumbnail_url)) AS youtubeFullImageUrls,
            group_concat(DISTINCT quote(Album_spotifyImage_uri)) AS spotifyFullImageUrls,
            COALESCE(SUM(Track_metadata_durationMs), SUM(Track_youtubeVideo_durationMs), 0) AS totalDurationMs
        FROM Artist
            JOIN AlbumArtist ON Artist_id = AlbumArtist_artistId        
            JOIN Album ON Album_albumId = AlbumArtist_albumId AND Album_isInLibrary = 1
            LEFT JOIN Track ON Track_albumId = Album_albumId AND Track_isInLibrary = 1
        GROUP BY Artist_id        
        ORDER BY LOWER(Artist_name)
        """
    )
    fun flowAlbumArtistCombos(): Flow<List<ArtistCombo>>

    @Query("SELECT * FROM Artist WHERE Artist_id = :id")
    fun flowArtistById(id: UUID): Flow<Artist?>

    @Query("SELECT * FROM Artist ORDER BY Artist_name")
    fun flowArtists(): Flow<List<Artist>>

    @Query(
        """
        SELECT
            Artist.*,
            COUNT(DISTINCT Track_trackId) AS trackCount,
            0 AS albumCount,
            group_concat(DISTINCT quote(Album_albumArt_uri)) AS albumArtUris,
            group_concat(DISTINCT quote(Album_youtubePlaylist_thumbnail_url)) AS youtubeFullImageUrls,
            group_concat(DISTINCT quote(Album_spotifyImage_uri)) AS spotifyFullImageUrls,
            COALESCE(SUM(Track_metadata_durationMs), 0) AS totalDurationMs
        FROM Artist
            JOIN TrackArtist ON Artist_id = TrackArtist_artistId        
            JOIN Track ON Track_trackId = TrackArtist_trackId AND Track_isInLibrary = 1
            LEFT JOIN Album ON Album_albumId = Track_albumId
        WHERE NOT EXISTS(
            SELECT * FROM AlbumArtist JOIN Album ON AlbumArtist_albumId = Album_albumId
            WHERE Album_isInLibrary = 1 AND AlbumArtist_artistId = Artist_id
        )
        GROUP BY Artist_id        
        ORDER BY LOWER(Artist_name)
        """
    )
    fun flowTrackArtistCombos(): Flow<List<ArtistCombo>>

    @Transaction
    suspend fun getOrCreateArtistByName(name: String): Artist =
        _getArtistByName(name) ?: Artist(name).also { _insertArtists(it) }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAlbumArtists(vararg albumArtists: AlbumArtist)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTrackArtists(vararg trackArtists: TrackArtist)

    @Query("SELECT * FROM Artist")
    suspend fun listArtists(): List<Artist>

    @Query(
        """
        SELECT Artist_id, Artist_name, Artist_spotifyId,
            (
                SELECT COUNT(Track_trackId) FROM Track
                LEFT JOIN TrackArtist ON Track_trackId = TrackArtist_trackId
                JOIN AlbumArtist ON Track_albumId = AlbumArtist_albumId AND TrackArtist_artistId = Artist_id OR
                    (AlbumArtist_artistId = Artist_id AND TrackArtist_artistId IS NULL)
            ) AS trackCount
        FROM Artist
        WHERE Artist_spotifyId IS NOT NULL
        ORDER BY trackCount DESC
        LIMIT :limit
        """
    )
    suspend fun listTopSpotifyArtists(limit: Int): List<TopLocalSpotifyArtistPojo>

    @Update
    suspend fun updateArtists(vararg artist: Artist)
}
