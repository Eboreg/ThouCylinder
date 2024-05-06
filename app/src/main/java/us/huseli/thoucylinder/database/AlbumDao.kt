@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import android.database.DatabaseUtils
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
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.AlbumArtist
import us.huseli.thoucylinder.dataclasses.entities.AlbumTag
import us.huseli.thoucylinder.dataclasses.entities.Tag
import us.huseli.thoucylinder.dataclasses.entities.toAlbumTags
import us.huseli.thoucylinder.dataclasses.pojos.TagPojo
import us.huseli.thoucylinder.dataclasses.views.AlbumCombo
import us.huseli.thoucylinder.enums.AlbumSortParameter
import us.huseli.thoucylinder.enums.AvailabilityFilter
import us.huseli.thoucylinder.enums.SortOrder

@Dao
abstract class AlbumDao {
    /** Protected methods *****************************************************/
    @Query("DELETE FROM AlbumTag WHERE AlbumTag_albumId IN (:albumIds)")
    protected abstract suspend fun _deleteAlbumTags(vararg albumIds: String)

    @Transaction
    @RawQuery(observedEntities = [Album::class, Tag::class, AlbumTag::class, AlbumArtist::class])
    protected abstract fun _flowAlbumCombos(query: SupportSQLiteQuery): Flow<List<AlbumCombo>>

    @RawQuery(observedEntities = [Tag::class, AlbumTag::class, Album::class])
    protected abstract fun _flowTagPojos(query: SupportSQLiteQuery): Flow<List<TagPojo>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun _insertAlbumTags(vararg albumTags: AlbumTag)

    @Query(
        """
        UPDATE Album SET Album_albumArt_fullUriString = :fullUri,
            Album_albumArt_thumbnailUriString = :thumbnailUri,
            Album_albumArt_hash = :hash 
        WHERE Album_albumId = :albumId
        """
    )
    protected abstract suspend fun _updateAlbumArt(albumId: String, fullUri: String, thumbnailUri: String, hash: Int)

    /** Public methods ********************************************************/
    @Query(
        """
        UPDATE Album SET Album_albumArt_fullUriString = NULL, Album_albumArt_thumbnailUriString = NULL,
            Album_albumArt_hash = NULL
        WHERE Album_albumId IN (:albumIds)
        """
    )
    abstract suspend fun clearAlbumArt(vararg albumIds: String)

    @Query("DELETE FROM Album")
    abstract suspend fun clearAlbums()

    @Query("DELETE FROM Tag")
    abstract suspend fun clearTags()

    @Delete
    abstract suspend fun deleteAlbums(vararg albums: Album)

    @Query("DELETE FROM Album WHERE Album_isInLibrary = 0")
    abstract suspend fun deleteTempAlbums()

    @Transaction
    @Query("SELECT * FROM AlbumCombo WHERE Album_albumId = :albumId")
    abstract fun flowAlbumCombo(albumId: String): Flow<AlbumCombo?>

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

    @Query(
        """
        SELECT AlbumCombo.* FROM AlbumCombo 
        JOIN AlbumArtistCredit ON Album_albumId = AlbumArtist_albumId AND AlbumArtist_artistId = :artistId
        WHERE Album_isInLibrary = 1
        GROUP BY Album_albumId
        ORDER BY LOWER(Album_title)
        """
    )
    @Transaction
    abstract fun flowAlbumCombosByArtist(artistId: String): Flow<List<AlbumCombo>>

    @Transaction
    @Query("SELECT * FROM Album WHERE Album_albumId = :albumId")
    abstract fun flowAlbumWithTracks(albumId: String): Flow<AlbumWithTracksCombo?>

    @Query(
        """
        SELECT Album_spotifyId FROM Album 
        WHERE Album_isInLibrary = 1 AND Album_isDeleted = 0 AND Album_spotifyId IS NOT NULL
        """
    )
    abstract fun flowSpotifyAlbumIds(): Flow<List<String>>

    @Query("SELECT Album_musicBrainzReleaseId FROM Album WHERE Album_isInLibrary = 1 AND Album_isDeleted = 0 AND Album_musicBrainzReleaseId IS NOT NULL")
    abstract fun flowMusicBrainzReleaseIds(): Flow<List<String>>

    fun flowTagPojos(availabilityFilter: AvailabilityFilter): Flow<List<TagPojo>> {
        val availabilityQuery = when (availabilityFilter) {
            AvailabilityFilter.ALL -> ""
            AvailabilityFilter.ONLY_PLAYABLE -> "AND (Album_isLocal = 1 OR Album_youtubePlaylist_id IS NOT NULL)"
            AvailabilityFilter.ONLY_LOCAL -> "AND Album_isLocal = 1"
        }

        return _flowTagPojos(
            SimpleSQLiteQuery(
                """
                SELECT Tag_name AS name, COUNT(*) AS itemCount
                FROM Tag JOIN AlbumTag ON Tag_name = AlbumTag_tagName
                JOIN Album ON Album_albumId = AlbumTag_albumId
                WHERE Album_isInLibrary = 1
                $availabilityQuery
                GROUP BY Tag_name
                ORDER BY itemCount DESC, Tag_name ASC
                """.trimIndent()
            )
        )
    }

    @Query("SELECT * FROM Tag ORDER BY Tag_name")
    abstract fun flowTags(): Flow<List<Tag>>

    @Query(
        """
        SELECT DISTINCT Tag.* FROM Tag JOIN AlbumTag ON Tag_name = AlbumTag_tagName
        WHERE AlbumTag_albumId = :albumId
        """
    )
    abstract fun flowTagsByAlbumId(albumId: String): Flow<List<Tag>>

    @Query("SELECT Album_youtubePlaylist_id FROM Album WHERE Album_isInLibrary = 1 AND Album_isDeleted = 0 AND Album_youtubePlaylist_id IS NOT NULL")
    abstract fun flowYoutubePlaylistIds(): Flow<List<String>>

    @Query("SELECT * FROM Album WHERE Album_albumId = :albumId")
    abstract suspend fun getAlbum(albumId: String): Album?

    @Query("SELECT * FROM AlbumCombo WHERE Album_albumId = :albumId")
    @Transaction
    abstract suspend fun getAlbumCombo(albumId: String): AlbumCombo?

    @Transaction
    @Query("SELECT * FROM Album WHERE Album_albumId = :albumId")
    abstract suspend fun getAlbumWithTracks(albumId: String): AlbumWithTracksCombo?

    @Transaction
    @Query("SELECT * FROM Album WHERE Album_youtubePlaylist_id = :playlistId")
    abstract suspend fun getAlbumWithTracksByPlaylistId(playlistId: String): AlbumWithTracksCombo?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertTags(vararg tags: Tag)

    @Transaction
    @Query("SELECT * FROM AlbumCombo")
    abstract suspend fun listAlbumCombos(): List<AlbumCombo>

    @Transaction
    @Query("SELECT * FROM AlbumCombo WHERE Album_albumId IN(:albumIds)")
    abstract suspend fun listAlbumCombos(vararg albumIds: String): List<AlbumCombo>

    @Query("SELECT * FROM Album WHERE Album_isInLibrary = 1")
    abstract suspend fun listAlbums(): List<Album>

    @Transaction
    @Query("SELECT * FROM Album")
    abstract suspend fun listAlbumsWithTracks(): List<AlbumWithTracksCombo>

    @Transaction
    @Query("SELECT * FROM Album WHERE Album_albumId IN(:albumIds)")
    abstract suspend fun listAlbumsWithTracks(vararg albumIds: String): List<AlbumWithTracksCombo>

    @Transaction
    @Query("SELECT * FROM Album WHERE Album_isDeleted = 1")
    abstract suspend fun listDeletionMarkedAlbumCombos(): List<AlbumWithTracksCombo>

    @Query("SELECT * FROM Tag")
    abstract suspend fun listTags(): List<Tag>

    @Query("SELECT DISTINCT Tag.* FROM Tag JOIN AlbumTag ON Tag_name = AlbumTag_tagName WHERE AlbumTag_albumId = :albumId")
    abstract suspend fun listTags(albumId: String): List<Tag>

    @Transaction
    open suspend fun setAlbumTags(albumId: String, tags: Collection<Tag>) {
        _deleteAlbumTags(albumId)
        if (tags.isNotEmpty()) {
            insertTags(*tags.toTypedArray())
            _insertAlbumTags(*tags.toAlbumTags(albumId).toTypedArray())
        }
    }

    @Query("UPDATE Album SET Album_isHidden = :isHidden WHERE Album_albumId IN (:albumIds)")
    abstract suspend fun setIsHidden(isHidden: Boolean, vararg albumIds: String)

    @Query("UPDATE Album SET Album_isInLibrary = :isInLibrary WHERE Album_albumId IN (:albumIds)")
    abstract suspend fun setIsInLibrary(isInLibrary: Boolean, vararg albumIds: String)

    @Query("UPDATE Album SET Album_isLocal = :isLocal WHERE Album_albumId IN (:albumIds)")
    abstract suspend fun setIsLocal(isLocal: Boolean, vararg albumIds: String)

    @Query("UPDATE Album SET Album_isHidden = 0 WHERE Album_isHidden = 1 AND Album_isLocal = 1")
    abstract suspend fun unhideLocalAlbums()

    suspend fun updateAlbumArt(albumId: String, albumArt: MediaStoreImage) =
        _updateAlbumArt(albumId, albumArt.fullUriString, albumArt.thumbnailUriString, albumArt.hash)

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
