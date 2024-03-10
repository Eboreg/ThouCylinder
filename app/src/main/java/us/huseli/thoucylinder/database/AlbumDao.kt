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
import us.huseli.thoucylinder.AvailabilityFilter
import us.huseli.thoucylinder.SortOrder
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.combos.AlbumCombo
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.AlbumArtist
import us.huseli.thoucylinder.dataclasses.entities.AlbumTag
import us.huseli.thoucylinder.dataclasses.entities.Tag
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.entities.toAlbumTags
import us.huseli.thoucylinder.dataclasses.pojos.TagPojo
import java.util.UUID

@Dao
abstract class AlbumDao {
    /** Protected methods *****************************************************/
    @Query("DELETE FROM AlbumTag WHERE AlbumTag_albumId IN (:albumIds)")
    protected abstract suspend fun _deleteAlbumTags(vararg albumIds: UUID)

    @Transaction
    @RawQuery(observedEntities = [Album::class, Tag::class, AlbumTag::class, AlbumArtist::class])
    protected abstract fun _flowAlbumCombos(query: SupportSQLiteQuery): Flow<List<AlbumCombo>>

    @RawQuery(observedEntities = [Tag::class, AlbumTag::class, Album::class])
    protected abstract fun _flowTagPojos(query: SupportSQLiteQuery): Flow<List<TagPojo>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun _insertAlbumTags(vararg albumTags: AlbumTag)

    @Query(
        """
        UPDATE Album SET Album_albumArt_uri = :uri, Album_albumArt_thumbnailUri = :thumbnailUri, Album_albumArt_hash = :hash 
        WHERE Album_albumId = :albumId
        """
    )
    protected abstract suspend fun _updateAlbumArt(albumId: UUID, uri: Uri, thumbnailUri: Uri, hash: Int)

    /** Public methods ********************************************************/
    @Query("UPDATE Album SET Album_albumArt_uri = NULL, Album_albumArt_thumbnailUri = NULL WHERE Album_albumId = :albumId")
    abstract suspend fun clearAlbumArt(albumId: UUID)

    @Query("DELETE FROM Album")
    abstract suspend fun clearAlbums()

    @Query("DELETE FROM Tag")
    abstract suspend fun clearTags()

    @Delete
    abstract suspend fun deleteAlbums(vararg albums: Album)

    @Query("DELETE FROM Album WHERE Album_isInLibrary = 0")
    abstract suspend fun deleteTempAlbums()

    @Query(
        """
        SELECT AlbumCombo.* FROM AlbumCombo 
        JOIN AlbumArtistCredit ON Album_albumId = AlbumArtist_albumId AND AlbumArtist_artistId = :artistId
        GROUP BY Album_albumId
        ORDER BY LOWER(Album_title)
        """
    )
    @Transaction
    abstract fun flowAlbumCombosByArtist(artistId: UUID): Flow<List<AlbumCombo>>

    fun flowAlbumCombos(
        sortParameter: AlbumSortParameter,
        sortOrder: SortOrder,
        searchTerm: String? = null,
        tagNames: Collection<String> = emptyList(),
        availabilityFilter: AvailabilityFilter,
    ): Flow<List<AlbumCombo>> {
        val searchQuery = searchTerm
            ?.takeIf { it.isNotBlank() }
            ?.lowercase()
            ?.split(Regex(" +"))
            ?.map { DatabaseUtils.sqlEscapeString("%$it%") }
            ?.joinToString(" AND ") { term ->
                "(LOWER(AlbumArtist_name) LIKE $term OR LOWER(Album_title) LIKE $term OR Album_year LIKE $term)"
            }
            ?.let { " AND $it" }
            ?: ""
        val tagList = tagNames.joinToString(", ") { DatabaseUtils.sqlEscapeString(it) }
        val tagJoin =
            if (tagNames.isNotEmpty())
                "JOIN AlbumTag ON Album_albumId = AlbumTag_albumId AND AlbumTag_tagName IN ($tagList)"
            else ""
        val availabilityQuery = when (availabilityFilter) {
            AvailabilityFilter.ALL -> ""
            AvailabilityFilter.ONLY_PLAYABLE -> "AND EXISTS(SELECT Track_trackId FROM Track WHERE Track_albumId = " +
                "Album_albumId AND (Track_localUri IS NOT NULL OR Track_youtubeVideo_id IS NOT NULL))"
            AvailabilityFilter.ONLY_LOCAL -> "AND EXISTS(SELECT Track_trackId FROM Track WHERE Track_albumId = " +
                "Album_albumId AND Track_localUri IS NOT NULL)"
        }

        return _flowAlbumCombos(
            SimpleSQLiteQuery(
                """
                SELECT AlbumCombo.* FROM AlbumCombo LEFT JOIN AlbumArtistCredit ON Album_albumId = AlbumArtist_albumId $tagJoin
                WHERE Album_isInLibrary = 1 AND Album_isDeleted = 0 AND Album_isHidden = 0 $searchQuery $availabilityQuery
                GROUP BY Album_albumId
                ORDER BY ${sortParameter.sql(sortOrder)}
                """.trimIndent()
            )
        )
    }

    @Transaction
    @Query("SELECT * FROM Album WHERE Album_albumId = :albumId")
    abstract fun flowAlbumWithTracks(albumId: UUID): Flow<AlbumWithTracksCombo?>

    fun flowTagPojos(availabilityFilter: AvailabilityFilter): Flow<List<TagPojo>> {
        val availabilityQuery = when (availabilityFilter) {
            AvailabilityFilter.ALL -> ""
            AvailabilityFilter.ONLY_PLAYABLE -> "WHERE Album_isLocal = 1 OR Album_youtubePlaylist_id IS NOT NULL"
            AvailabilityFilter.ONLY_LOCAL -> "WHERE Album_isLocal = 1"
        }

        return _flowTagPojos(
            SimpleSQLiteQuery(
                """
                SELECT Tag_name AS name, COUNT(*) AS itemCount
                FROM Tag JOIN AlbumTag ON Tag_name = AlbumTag_tagName
                JOIN Album ON Album_albumId = AlbumTag_albumId
                $availabilityQuery
                GROUP BY Tag_name
                ORDER BY itemCount DESC, Tag_name ASC
                """.trimIndent()
            )
        )
    }

    @Query("SELECT * FROM Tag ORDER BY Tag_name")
    abstract fun flowTags(): Flow<List<Tag>>

    @Query("SELECT * FROM AlbumCombo WHERE Album_albumId = :albumId")
    @Transaction
    abstract suspend fun getAlbumCombo(albumId: UUID): AlbumCombo?

    @Transaction
    @Query("SELECT * FROM Album WHERE Album_albumId = :albumId")
    abstract suspend fun getAlbumWithTracks(albumId: UUID): AlbumWithTracksCombo?

    @Transaction
    @Query("SELECT * FROM Album WHERE Album_youtubePlaylist_id = :playlistId")
    abstract suspend fun getAlbumWithTracksByPlaylistId(playlistId: String): AlbumWithTracksCombo?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertTags(vararg tags: Tag)

    @Transaction
    @Query("SELECT * FROM AlbumCombo")
    abstract suspend fun listAlbumCombos(): List<AlbumCombo>

    @Query("SELECT * FROM Album WHERE Album_isInLibrary = 1")
    abstract suspend fun listAlbums(): List<Album>

    @Transaction
    @Query("SELECT * FROM Album WHERE Album_albumId IN(:albumIds)")
    abstract suspend fun listAlbumsWithTracks(vararg albumIds: UUID): List<AlbumWithTracksCombo>

    @Transaction
    @Query("SELECT * FROM AlbumCombo WHERE Album_isDeleted = 1")
    abstract suspend fun listDeletionMarkedAlbumCombos(): List<AlbumCombo>

    @Query("SELECT * FROM Tag")
    abstract suspend fun listTags(): List<Tag>

    @Query(
        """
        SELECT Album_spotifyId FROM Album 
        WHERE Album_isInLibrary = 1 AND Album_isDeleted = 0 AND Album_spotifyId IS NOT NULL
        """
    )
    abstract suspend fun listImportedSpotifyIds(): List<String>

    @Query("SELECT Album_musicBrainzReleaseId FROM Album WHERE Album_musicBrainzReleaseId IS NOT NULL")
    abstract suspend fun listMusicBrainzReleaseIds(): List<String>

    @Query("SELECT * FROM Track WHERE Track_albumId = :albumId ORDER BY Track_discNumber, Track_albumPosition")
    abstract suspend fun listTracks(albumId: UUID): List<Track>

    @Transaction
    open suspend fun setAlbumTags(albumId: UUID, tags: Collection<Tag>) {
        _deleteAlbumTags(albumId)
        if (tags.isNotEmpty()) {
            insertTags(*tags.toTypedArray())
            _insertAlbumTags(*tags.toAlbumTags(albumId).toTypedArray())
        }
    }

    @Query("UPDATE Album SET Album_isHidden = :isHidden WHERE Album_albumId IN (:albumIds)")
    abstract suspend fun setIsHidden(isHidden: Boolean, vararg albumIds: UUID)

    @Query("UPDATE Album SET Album_isInLibrary = :isInLibrary WHERE Album_albumId IN (:albumIds)")
    abstract suspend fun setIsInLibrary(isInLibrary: Boolean, vararg albumIds: UUID)

    @Query("UPDATE Album SET Album_isLocal = :isLocal WHERE Album_albumId IN (:albumIds)")
    abstract suspend fun setIsLocal(isLocal: Boolean, vararg albumIds: UUID)

    suspend fun updateAlbumArt(albumId: UUID, albumArt: MediaStoreImage) =
        _updateAlbumArt(albumId, albumArt.uri, albumArt.thumbnailUri, albumArt.hash)

    @Update
    abstract suspend fun updateAlbums(vararg albums: Album)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertAlbums(vararg albums: Album)

    @Transaction
    open suspend fun upsertAlbumsAndTags(combos: Collection<AlbumWithTracksCombo>) {
        if (combos.isNotEmpty()) {
            val tags = combos.flatMap { it.tags }

            upsertAlbums(*combos.map { it.album }.toTypedArray())
            _deleteAlbumTags(*combos.map { it.album.albumId }.toTypedArray())
            if (tags.isNotEmpty()) {
                insertTags(*tags.toTypedArray())
                _insertAlbumTags(*combos.flatMap { it.albumTags }.toTypedArray())
            }
        }
    }
}
