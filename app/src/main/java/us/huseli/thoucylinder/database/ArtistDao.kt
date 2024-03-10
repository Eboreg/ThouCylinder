@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import android.net.Uri
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import us.huseli.thoucylinder.dataclasses.abstr.BaseArtist
import us.huseli.thoucylinder.dataclasses.combos.ArtistCombo
import us.huseli.thoucylinder.dataclasses.entities.AlbumArtist
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.entities.TrackArtist
import us.huseli.thoucylinder.dataclasses.pojos.TopLocalSpotifyArtistPojo
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyArtist
import us.huseli.thoucylinder.dataclasses.spotify.toMediaStoreImage
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit
import java.util.UUID

@Dao
abstract class ArtistDao {
    @Query("SELECT * FROM Artist WHERE LOWER(Artist_name) = LOWER(:name) LIMIT 1")
    protected abstract suspend fun _getArtistByName(name: String): Artist?

    @Insert
    protected abstract suspend fun _insertArtists(vararg artists: Artist)

    @Update
    protected abstract suspend fun _updateArtists(vararg artists: Artist)

    @Query("DELETE FROM AlbumArtist WHERE AlbumArtist_albumId IN(:albumIds)")
    abstract suspend fun clearAlbumArtists(vararg albumIds: UUID)

    @Query("DELETE FROM Artist")
    abstract suspend fun clearArtists()

    @Query("DELETE FROM TrackArtist WHERE TrackArtist_trackId IN(:trackIds)")
    abstract suspend fun clearTrackArtists(vararg trackIds: UUID)

    @Query(
        """
        SELECT
            Artist.*,
            COUNT(DISTINCT Track_trackId) AS trackCount,
            COUNT(DISTINCT Album_albumId) AS albumCount,
            group_concat(DISTINCT quote(Album_albumArt_uri)) AS albumArtUris,
            group_concat(DISTINCT quote(Album_youtubePlaylist_thumbnail_url)) AS youtubeFullImageUrls,
            group_concat(DISTINCT quote(Album_spotifyImage_uri)) AS spotifyFullImageUrls
        FROM Artist
            JOIN AlbumArtist ON Artist_id = AlbumArtist_artistId        
            JOIN Album ON Album_albumId = AlbumArtist_albumId AND Album_isInLibrary = 1
            LEFT JOIN Track ON Track_albumId = Album_albumId AND Track_isInLibrary = 1
        GROUP BY Artist_id        
        ORDER BY LOWER(Artist_name)
        """
    )
    abstract fun flowAlbumArtistCombos(): Flow<List<ArtistCombo>>

    @Query("SELECT * FROM Artist WHERE Artist_id = :id")
    abstract fun flowArtistById(id: UUID): Flow<Artist?>

    @Query("SELECT * FROM Artist ORDER BY Artist_name")
    abstract fun flowArtists(): Flow<List<Artist>>

    @Query(
        """
        SELECT Artist.* FROM Artist
            LEFT JOIN AlbumArtist ON Artist_id = AlbumArtist_artistId
            LEFT JOIN TrackArtist ON Artist_id = TrackArtist_artistId
        GROUP BY Artist_id
        HAVING AlbumArtist_artistId IS NOT NULL OR TrackArtist_artistId IS NOT NULL
        """
    )
    abstract fun flowArtistsWithTracksOrAlbums(): Flow<List<Artist>>

    @Query(
        """
        SELECT
            Artist.*,
            COUNT(DISTINCT Track_trackId) AS trackCount,
            0 AS albumCount,
            group_concat(DISTINCT quote(Album_albumArt_uri)) AS albumArtUris,
            group_concat(DISTINCT quote(Album_youtubePlaylist_thumbnail_url)) AS youtubeFullImageUrls,
            group_concat(DISTINCT quote(Album_spotifyImage_uri)) AS spotifyFullImageUrls
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
    abstract fun flowTrackArtistCombos(): Flow<List<ArtistCombo>>

    @Query("SELECT * FROM Artist WHERE Artist_id = :id")
    abstract suspend fun getArtist(id: UUID): Artist?

    @Transaction
    open suspend fun createOrUpdateArtist(baseArtist: BaseArtist): Artist {
        return _getArtistByName(baseArtist.name)?.let { artist ->
            if (
                (baseArtist.image != null && baseArtist.image != artist.image) ||
                (baseArtist.musicBrainzId != null && baseArtist.musicBrainzId != artist.musicBrainzId) ||
                (baseArtist.spotifyId != null && baseArtist.spotifyId != artist.spotifyId)
            ) {
                artist.copy(
                    image = baseArtist.image ?: artist.image,
                    musicBrainzId = baseArtist.musicBrainzId ?: artist.musicBrainzId,
                    spotifyId = baseArtist.spotifyId ?: artist.spotifyId,
                ).also { _updateArtists(it) }
            } else artist
        } ?: Artist.fromBase(baseArtist).also { _insertArtists(it) }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertAlbumArtists(vararg albumArtists: AlbumArtist)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertTrackArtists(vararg trackArtists: TrackArtist)

    @Query(
        """
        SELECT Artist_id, Artist_name, Artist_spotifyId, COUNT(DISTINCT Track_trackId) AS trackCount
        FROM Artist 
            LEFT JOIN TrackArtist ON Artist_id = TrackArtist_artistId
            LEFT JOIN AlbumArtist ON Artist_id = AlbumArtist_artistId
            LEFT JOIN Track ON Track_trackId = TrackArtist_trackId OR Track_albumId = AlbumArtist_albumId
        WHERE Artist_spotifyId IS NOT NULL AND Artist_spotifyId != "" AND Artist_isVarious == 0
        GROUP BY Artist_id
        ORDER BY trackCount DESC
        LIMIT :limit
        """
    )
    abstract suspend fun listTopSpotifyArtists(limit: Int): List<TopLocalSpotifyArtistPojo>

    @Query("SELECT * FROM TrackArtistCredit WHERE TrackArtist_trackId = :trackId ORDER BY TrackArtist_position")
    abstract suspend fun listTrackArtistCredits(trackId: UUID): List<TrackArtistCredit>

    @Query("UPDATE Artist SET Artist_musicBrainzId = :musicBrainzId WHERE Artist_id = :artistId")
    abstract suspend fun setMusicBrainzId(artistId: UUID, musicBrainzId: String)

    @Query(
        """
        UPDATE Artist
        SET Artist_spotifyId = :spotifyId,
            Artist_image_uri = :imageUri,
            Artist_image_thumbnailUri = :imageThumbnailUri,
            Artist_image_hash = :imageHash
        WHERE Artist_id = :artistId
        """
    )
    abstract suspend fun setSpotifyData(
        artistId: UUID,
        spotifyId: String,
        imageUri: Uri?,
        imageThumbnailUri: Uri?,
        imageHash: Int?,
    )

    @Query("UPDATE Artist SET Artist_spotifyId = :spotifyId WHERE Artist_id = :artistId")
    abstract suspend fun setSpotifyId(artistId: UUID, spotifyId: String)

    suspend fun upsertSpotifyArtist(spotifyArtist: SpotifyArtist) {
        val artist = _getArtistByName(spotifyArtist.name)
        val image = spotifyArtist.images.toMediaStoreImage()

        if (artist != null) setSpotifyData(artist.id, spotifyArtist.id, image?.uri, image?.thumbnailUri, image?.hash)
        else _insertArtists(Artist(name = spotifyArtist.name, spotifyId = spotifyArtist.id, image = image))
    }
}
