@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import android.database.DatabaseUtils
import android.net.Uri
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import us.huseli.thoucylinder.AlbumSortParameter
import us.huseli.thoucylinder.SortOrder
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.combos.AlbumCombo
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.combos.TrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.AlbumTag
import us.huseli.thoucylinder.dataclasses.entities.Tag
import us.huseli.thoucylinder.dataclasses.entities.Track
import java.util.UUID

@Dao
interface AlbumDao {
    /** Pseudo-private methods ************************************************/
    @Delete
    suspend fun _deleteAlbumTags(vararg albumTags: AlbumTag)

    @RawQuery(
        observedEntities = [
            Album::class,
            Track::class,
            Tag::class,
            AlbumTag::class,
        ],
    )
    fun _flowAlbumCombos(query: SupportSQLiteQuery): Flow<List<AlbumCombo>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun _insertAlbumTags(vararg albumTags: AlbumTag)

    @Query("SELECT Tag.* FROM Tag JOIN AlbumTag ON Tag_name = AlbumTag_tagName AND AlbumTag_albumId = :albumId")
    suspend fun _listTagsByAlbum(albumId: UUID): List<Tag>

    @Query(
        """
        UPDATE Album SET Album_albumArt_uri = :uri, Album_albumArt_thumbnailUri = :thumbnailUri, Album_albumArt_hash = :hash 
        WHERE Album_albumId = :albumId
        """
    )
    suspend fun _updateAlbumArt(albumId: UUID, uri: Uri, thumbnailUri: Uri, hash: Int)

    /** Public methods ********************************************************/
    @Query("UPDATE Album SET Album_albumArt_uri = NULL, Album_albumArt_thumbnailUri = NULL WHERE Album_albumId = :albumId")
    suspend fun clearAlbumArt(albumId: UUID)

    @Delete
    suspend fun deleteAlbums(vararg albums: Album)

    @Query("DELETE FROM Album WHERE Album_isInLibrary = 0")
    suspend fun deleteTempAlbums()

    fun flowAlbumCombosByArtist(artist: String) = flowAlbumCombos(
        sortParameter = AlbumSortParameter.TITLE,
        sortOrder = SortOrder.ASCENDING,
        artist = artist,
    )

    fun flowAlbumCombos(
        sortParameter: AlbumSortParameter,
        sortOrder: SortOrder,
        artist: String? = null,
        searchTerm: String? = null,
    ): Flow<List<AlbumCombo>> {
        val searchQuery = searchTerm
            ?.lowercase()
            ?.split(Regex(" +"))
            ?.takeIf { it.isNotEmpty() }
            ?.map { DatabaseUtils.sqlEscapeString("%$it%") }
            ?.joinToString(" AND ") { term ->
                "(LOWER(Album_artist) LIKE $term OR LOWER(Album_title) LIKE $term OR Album_year LIKE $term)"
            }

        return _flowAlbumCombos(
            SimpleSQLiteQuery(
                """
                SELECT Album.*, 
                    SUM(COALESCE(Track_metadata_durationMs, Track_youtubeVideo_durationMs, Track_youtubeVideo_metadata_durationMs)) AS durationMs,
                    MIN(Track_year) AS minYear, MAX(Track_year) AS maxYear, COUNT(Track_trackId) AS trackCount,
                    EXISTS(SELECT * FROM Track WHERE Track_albumId = Album_albumId AND Track_localUri IS NOT NULL) AND 
                        EXISTS(SELECT * FROM Track WHERE Track_albumId = Album_albumId AND Track_localUri IS NULL)
                        AS isPartiallyDownloaded
                FROM Album LEFT JOIN Track ON Album_albumId = Track_albumId 
                WHERE Album_isInLibrary = 1 AND Album_isDeleted = 0 AND Album_isHidden = 0
                    ${artist?.let { "AND LOWER(Album_artist) = LOWER(\"$it\")" } ?: ""}
                    ${searchQuery?.let { "AND $it" } ?: ""}                                
                GROUP BY Album_albumId
                ORDER BY ${sortParameter.sqlColumn} ${if (sortOrder == SortOrder.ASCENDING) "ASC" else "DESC"}
                """.trimIndent()
            )
        )
    }

    @Query("SELECT * FROM Album WHERE Album_albumId = :albumId")
    @Transaction
    fun flowAlbumWithTracks(albumId: UUID): Flow<AlbumWithTracksCombo?>

    @Query("SELECT * FROM Tag ORDER BY Tag_name")
    fun flowTags(): Flow<List<Tag>>

    @Query("SELECT * FROM Album WHERE Album_albumId = :albumId")
    suspend fun getAlbum(albumId: UUID): Album?

    @Query("SELECT * FROM Album WHERE Album_albumId = :albumId")
    @Transaction
    suspend fun getAlbumWithTracks(albumId: UUID): AlbumWithTracksCombo?

    @Transaction
    suspend fun insertAlbumCombos(combos: Collection<AbstractAlbumCombo>) {
        val tags = combos.flatMap { it.tags }

        insertAlbums(*combos.map { it.album }.toTypedArray())
        insertTags(*tags.toTypedArray())
        _insertAlbumTags(*combos.flatMap { it.albumTags }.toTypedArray())
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbums(vararg albums: Album)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTags(vararg tags: Tag)

    @Query("SELECT * FROM Album WHERE Album_isInLibrary = 1")
    suspend fun listAlbums(): List<Album>

    @Query("SELECT * FROM Album WHERE Album_isDeleted = 1")
    suspend fun listDeletionMarkedAlbums(): List<Album>

    @Query("SELECT * FROM Tag")
    suspend fun listTags(): List<Tag>

    @Query(
        """
        SELECT Album_spotifyId FROM Album 
        WHERE Album_isInLibrary = 1 AND Album_isDeleted = 0 AND Album_spotifyId IS NOT NULL
        """
    )
    suspend fun listImportedSpotifyIds(): List<String>

    @Query("SELECT Album_musicBrainzReleaseId FROM Album WHERE Album_musicBrainzReleaseId IS NOT NULL")
    suspend fun listMusicBrainzReleaseIds(): List<String>

    @Query(
        """
        SELECT DISTINCT Track.*, Album.*
        FROM Track LEFT JOIN Album ON Track_albumId = Album_albumId
        WHERE Track_albumId IN (:albumIds)
        ORDER BY Track_albumId, Track_discNumber, Track_albumPosition
        """
    )
    suspend fun listTrackCombos(albumIds: List<UUID>): List<TrackCombo>

    @Query("SELECT * FROM Track WHERE Track_albumId = :albumId ORDER BY Track_discNumber, Track_albumPosition")
    suspend fun listTracks(albumId: UUID): List<Track>

    @Query("UPDATE Album SET Album_isDeleted = :isDeleted WHERE Album_albumId = :albumId")
    suspend fun setIsDeleted(albumId: UUID, isDeleted: Boolean)

    @Query("UPDATE Album SET Album_isHidden = :value WHERE Album_albumId = :albumId")
    suspend fun setIsHidden(albumId: UUID, value: Boolean)

    @Query("UPDATE Album SET Album_isInLibrary = :isInLibrary WHERE Album_albumId IN (:albumIds)")
    suspend fun setIsInLibrary(albumIds: Collection<UUID>, isInLibrary: Boolean)

    @Query("UPDATE Album SET Album_isLocal = :isLocal WHERE Album_albumId IN (:albumIds)")
    suspend fun setIsLocal(albumIds: Collection<UUID>, isLocal: Boolean)

    suspend fun updateAlbumArt(albumId: UUID, albumArt: MediaStoreImage) =
        _updateAlbumArt(albumId, albumArt.uri, albumArt.thumbnailUri, albumArt.hash)

    suspend fun updateAlbumCombo(combo: AbstractAlbumCombo) {
        val albumTags = _listTagsByAlbum(combo.album.albumId)
        val tagsToDelete = albumTags
            .filter { !combo.tags.contains(it) }
            .map { AlbumTag(albumId = combo.album.albumId, tagName = it.name) }
        val tagsToAdd = combo.tags.filter { !albumTags.contains(it) }

        updateAlbums(combo.album)
        if (tagsToDelete.isNotEmpty()) _deleteAlbumTags(*tagsToDelete.toTypedArray())
        if (tagsToAdd.isNotEmpty()) {
            insertTags(*tagsToAdd.toTypedArray())
            _insertAlbumTags(
                *tagsToAdd.map { AlbumTag(albumId = combo.album.albumId, tagName = it.name) }.toTypedArray()
            )
        }
    }

    @Update
    suspend fun updateAlbums(vararg albums: Album)
}
