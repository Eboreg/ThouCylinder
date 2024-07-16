@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import us.huseli.thoucylinder.dataclasses.artist.AlbumArtist
import us.huseli.thoucylinder.dataclasses.artist.AlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.Artist
import us.huseli.thoucylinder.dataclasses.artist.ArtistCombo
import us.huseli.thoucylinder.dataclasses.artist.ArtistWithCounts
import us.huseli.thoucylinder.dataclasses.artist.IAlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.IArtist
import us.huseli.thoucylinder.dataclasses.artist.ISavedArtist
import us.huseli.thoucylinder.dataclasses.artist.ITrackArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.TrackArtist
import us.huseli.thoucylinder.dataclasses.artist.TrackArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.toAlbumArtists
import us.huseli.thoucylinder.dataclasses.artist.toTrackArtists

@Dao
abstract class ArtistDao {
    @Query("DELETE FROM AlbumArtist WHERE AlbumArtist_albumId = :albumId")
    protected abstract suspend fun _clearAlbumArtists(albumId: String)

    @Query("SELECT * FROM Artist WHERE LOWER(Artist_name) = LOWER(:name) LIMIT 1")
    protected abstract suspend fun _getArtistByLowerCaseName(name: String): Artist?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun _insertAlbumArtists(vararg albumArtists: AlbumArtist)

    @Insert
    protected abstract suspend fun _insertArtists(vararg artists: Artist)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun _insertTrackArtists(vararg trackArtists: TrackArtist)

    @Query("SELECT * FROM Artist WHERE Artist_id IN (:artistIds)")
    protected abstract suspend fun _listArtistsById(vararg artistIds: String): List<Artist>

    @Query("SELECT * FROM Artist WHERE LOWER(Artist_name) IN (:names)")
    protected abstract suspend fun _listArtistsByLowerCaseName(vararg names: String): List<Artist>

    private suspend fun _saveArtists(artists: Collection<IArtist>): List<Artist> {
        val savedArtists = artists.filterIsInstance<Artist>()
        val unsavedArtists = artists.filter { it !is Artist }
        val changedArtists = mutableListOf<Artist>()
        val result = mutableListOf<Artist>()

        if (unsavedArtists.isNotEmpty()) {
            val dbArtists =
                _listArtistsByLowerCaseName(*unsavedArtists.map { it.name.lowercase() }.toSet().toTypedArray())
            val existingArtists = unsavedArtists.mapNotNull { unsaved ->
                dbArtists
                    .find { it.name.lowercase() == unsaved.name.lowercase() }
                    ?.updateWith(unsaved)
            }.toSet()
            val newArtists = unsavedArtists
                .filter { a1 -> a1.name.lowercase() !in existingArtists.map { a2 -> a2.name.lowercase() } }
                .map { it.toSaveableArtist() }

            if (newArtists.isNotEmpty()) _insertArtists(*newArtists.toTypedArray())
            result.addAll(newArtists)
            result.addAll(existingArtists)
            changedArtists.addAll(existingArtists.filter { it !in dbArtists })
        }

        if (savedArtists.isNotEmpty()) {
            val dbArtists = _listArtistsById(*savedArtists.map { it.artistId }.toTypedArray())
            changedArtists.addAll(savedArtists.filter { it !in dbArtists })
            result.addAll(savedArtists)
        }
        if (changedArtists.isNotEmpty()) _updateArtists(*changedArtists.toTypedArray())

        return result.toList()
    }

    @Update
    protected abstract suspend fun _updateArtists(vararg artists: Artist)

    @Query("DELETE FROM Artist")
    abstract suspend fun clearArtists()

    @Query("DELETE FROM TrackArtist WHERE TrackArtist_trackId IN (:trackIds)")
    abstract suspend fun clearTrackArtists(vararg trackIds: String)

    @Query(
        """
        DELETE FROM Artist WHERE Artist_id NOT IN
        (SELECT TrackArtist_artistId FROM TrackArtist UNION SELECT AlbumArtist_artistId FROM AlbumArtist)
        """
    )
    abstract suspend fun deleteOrphanArtists()

    @Query("SELECT * FROM ArtistCombo WHERE Artist_id = :id")
    abstract fun flowArtistComboById(id: String): Flow<ArtistCombo?>

    @Query("SELECT * FROM ArtistCombo WHERE ArtistCombo_trackCount > 0 OR ArtistCombo_albumCount > 0")
    abstract fun flowArtistCombos(): Flow<List<ArtistCombo>>

    @Query("SELECT * FROM Artist ORDER BY Artist_name")
    abstract fun flowArtists(): Flow<List<Artist>>

    @Query(
        """
        SELECT Artist.*, COUNT(DISTINCT Track_trackId) AS trackCount, COUNT(DISTINCT Album_albumId) AS albumCount
        FROM Artist
            LEFT JOIN AlbumArtist ON AlbumArtist_artistId = Artist_id
            LEFT JOIN Album ON Album_albumId = AlbumArtist_albumId AND Album_albumId = :albumId
            LEFT JOIN TrackArtist ON TrackArtist_artistId = Artist_id
            LEFT JOIN Track ON Track_trackId = TrackArtist_trackId AND Track_albumId = :albumId
        WHERE Album_albumId = :albumId OR Track_albumId = :albumId
        GROUP BY Artist_id
        ORDER BY albumCount DESC, trackCount DESC
        """
    )
    abstract fun flowArtistsByAlbumId(albumId: String): Flow<List<ArtistWithCounts>>

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
    open suspend fun insertAlbumArtists(artistCredits: Collection<IAlbumArtistCredit>): List<AlbumArtistCredit> {
        val artists = _saveArtists(artistCredits)
        val albumArtistCredits = artistCredits.mapNotNull { artistCredit ->
            artists
                .find { it.name == artistCredit.name }
                ?.let { artistCredit.withArtistId(it.artistId) }
        }

        _insertAlbumArtists(*albumArtistCredits.toAlbumArtists().toTypedArray())
        return albumArtistCredits
    }

    @Transaction
    open suspend fun insertTrackArtists(artistCredits: Collection<ITrackArtistCredit>): List<TrackArtistCredit> {
        val artists = _saveArtists(artistCredits)
        val trackArtistCredits = artistCredits.mapNotNull { artistCredit ->
            artists
                .find { it.name.lowercase() == artistCredit.name.lowercase() }
                ?.let { artistCredit.withArtistId(it.artistId) }
        }

        _insertTrackArtists(*trackArtistCredits.toTrackArtists().toTypedArray())
        return trackArtistCredits
    }

    @Query("SELECT Artist.* FROM Artist JOIN TrackArtist ON Artist_id = TrackArtist_artistId WHERE TrackArtist_trackId = :trackId")
    abstract suspend fun listArtistsByTrackId(trackId: String): List<Artist>

    @Transaction
    open suspend fun setAlbumArtists(
        albumId: String,
        albumArtists: Collection<IAlbumArtistCredit>,
    ): List<AlbumArtistCredit> {
        _clearAlbumArtists(albumId)
        return insertAlbumArtists(albumArtists)
    }

    @Query("UPDATE Artist SET Artist_musicBrainzId = :musicBrainzId WHERE Artist_id = :artistId")
    abstract suspend fun setMusicBrainzId(artistId: String, musicBrainzId: String)

    @Query(
        """
        UPDATE Artist
        SET Artist_spotifyId = :spotifyId,
            Artist_image_fullUriString = :imageUri,
            Artist_image_thumbnailUriString = :imageThumbnailUri
        WHERE Artist_id = :artistId
        """
    )
    abstract suspend fun setSpotifyData(
        artistId: String,
        spotifyId: String,
        imageUri: String?,
        imageThumbnailUri: String?,
    )

    @Transaction
    open suspend fun setTrackArtists(trackId: String, trackArtists: Collection<ITrackArtistCredit>) {
        clearTrackArtists(trackId)
        insertTrackArtists(trackArtists)
    }

    @Transaction
    open suspend fun upsertArtist(artist: IArtist): Artist {
        val existingArtist =
            if (artist is ISavedArtist) getArtist(artist.artistId)
            else _getArtistByLowerCaseName(artist.name.lowercase())

        return existingArtist?.copy(
            name = artist.name,
            spotifyId = artist.spotifyId ?: existingArtist.spotifyId,
            musicBrainzId = artist.musicBrainzId ?: existingArtist.musicBrainzId,
            image = artist.image ?: existingArtist.image,
        )?.also { _updateArtists(it) } ?: artist.toSaveableArtist().also { _insertArtists(it) }
    }
}
