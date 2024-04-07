@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import us.huseli.thoucylinder.dataclasses.UnsavedArtist
import us.huseli.thoucylinder.dataclasses.entities.AlbumArtist
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.entities.TrackArtist
import us.huseli.thoucylinder.dataclasses.pojos.TopLocalSpotifyArtistPojo
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyArtist
import us.huseli.thoucylinder.dataclasses.spotify.toMediaStoreImage
import us.huseli.thoucylinder.dataclasses.views.ArtistCombo
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit

@Dao
abstract class ArtistDao {
    @Query("SELECT * FROM Artist WHERE LOWER(Artist_name) = LOWER(:name) LIMIT 1")
    protected abstract suspend fun _getArtistByName(name: String): Artist?

    @Insert
    protected abstract suspend fun _insertArtists(vararg artists: Artist)

    @Update
    protected abstract suspend fun _updateArtists(vararg artists: Artist)

    @Query("DELETE FROM AlbumArtist WHERE AlbumArtist_albumId IN(:albumIds)")
    abstract suspend fun clearAlbumArtists(vararg albumIds: String)

    @Query("DELETE FROM Artist")
    abstract suspend fun clearArtists()

    @Query("DELETE FROM TrackArtist WHERE TrackArtist_trackId IN(:trackIds)")
    abstract suspend fun clearTrackArtists(vararg trackIds: String)

    @Query("SELECT * FROM Artist WHERE Artist_id = :id")
    abstract fun flowArtistById(id: String): Flow<Artist?>

    @Query("SELECT * FROM ArtistCombo")
    abstract fun flowArtistCombos(): Flow<List<ArtistCombo>>

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

    @Query("SELECT * FROM Artist WHERE Artist_id = :id")
    abstract suspend fun getArtist(id: String): Artist?

    @Transaction
    open suspend fun createOrUpdateArtist(unsavedArtist: UnsavedArtist): Artist {
        return _getArtistByName(unsavedArtist.name)?.let { artist ->
            if (
                (unsavedArtist.image != null && unsavedArtist.image != artist.image) ||
                (unsavedArtist.musicBrainzId != null && unsavedArtist.musicBrainzId != artist.musicBrainzId) ||
                (unsavedArtist.spotifyId != null && unsavedArtist.spotifyId != artist.spotifyId)
            ) {
                artist.copy(
                    image = unsavedArtist.image ?: artist.image,
                    musicBrainzId = unsavedArtist.musicBrainzId ?: artist.musicBrainzId,
                    spotifyId = unsavedArtist.spotifyId ?: artist.spotifyId,
                ).also { _updateArtists(it) }
            } else artist
        } ?: Artist.fromBase(unsavedArtist).also { _insertArtists(it) }
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
        WHERE Artist_spotifyId IS NOT NULL AND Artist_spotifyId != ""
        GROUP BY Artist_id
        ORDER BY trackCount DESC
        LIMIT :limit
        """
    )
    abstract suspend fun listTopSpotifyArtists(limit: Int): List<TopLocalSpotifyArtistPojo>

    @Query("SELECT * FROM TrackArtistCredit WHERE TrackArtist_trackId = :trackId ORDER BY TrackArtist_position")
    abstract suspend fun listTrackArtistCredits(trackId: String): List<TrackArtistCredit>

    @Query("UPDATE Artist SET Artist_musicBrainzId = :musicBrainzId WHERE Artist_id = :artistId")
    abstract suspend fun setMusicBrainzId(artistId: String, musicBrainzId: String)

    @Query(
        """
        UPDATE Artist
        SET Artist_spotifyId = :spotifyId,
            Artist_image_fullUriString = :imageUri,
            Artist_image_thumbnailUriString = :imageThumbnailUri,
            Artist_image_hash = :imageHash
        WHERE Artist_id = :artistId
        """
    )
    abstract suspend fun setSpotifyData(
        artistId: String,
        spotifyId: String,
        imageUri: String?,
        imageThumbnailUri: String?,
        imageHash: Int?,
    )

    @Query("UPDATE Artist SET Artist_spotifyId = :spotifyId WHERE Artist_id = :artistId")
    abstract suspend fun setSpotifyId(artistId: String, spotifyId: String)

    suspend fun upsertSpotifyArtist(spotifyArtist: SpotifyArtist) {
        val artist = _getArtistByName(spotifyArtist.name)
        val image = spotifyArtist.images.toMediaStoreImage()

        if (artist != null) setSpotifyData(
            artistId = artist.artistId,
            spotifyId = spotifyArtist.id,
            imageUri = image?.fullUriString,
            imageThumbnailUri = image?.thumbnailUriString,
            imageHash = image?.hash,
        )
        else _insertArtists(Artist(name = spotifyArtist.name, spotifyId = spotifyArtist.id, image = image))
    }
}
