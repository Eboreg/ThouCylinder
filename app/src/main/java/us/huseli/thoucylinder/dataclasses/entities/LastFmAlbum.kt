package us.huseli.thoucylinder.dataclasses.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import us.huseli.thoucylinder.dataclasses.lastFm.LastFmAlbumResponse
import us.huseli.thoucylinder.dataclasses.lastFm.LastFmArtist
import us.huseli.thoucylinder.dataclasses.lastFm.LastFmWiki
import java.util.UUID

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Album::class,
            parentColumns = ["Album_albumId"],
            childColumns = ["LastFmAlbum_albumId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("LastFmAlbum_albumId")],
)
data class LastFmAlbum(
    @ColumnInfo("LastFmAlbum_musicBrainzId") @PrimaryKey val musicBrainzId: String,
    @Embedded("LastFmAlbum_artist_") val artist: LastFmArtist,
    @ColumnInfo("LastFmAlbum_name") val name: String,
    @Ignore val tracks: List<LastFmAlbumResponse.Album.Tracks.Track>,
    @ColumnInfo("LastFmAlbum_listeners") val listeners: Int?,
    @ColumnInfo("LastFmAlbum_fullImageUrl") val fullImageUrl: String?,
    @ColumnInfo("LastFmAlbum_thumbnailUrl") val thumbnailUrl: String?,
    @ColumnInfo("LastFmAlbum_playCount") val playCount: Int?,
    @ColumnInfo("LastFmAlbum_url") val url: String,
    @ColumnInfo("LastFmAlbum_albumId") val albumId: UUID? = null,
    @Embedded("LastFmAlbum_wiki_") val wiki: LastFmWiki?,
    @ColumnInfo("LastFmAlbum_tags") val tags: List<String>,
) {
    constructor(
        musicBrainzId: String,
        artist: LastFmArtist,
        name: String,
        listeners: Int?,
        fullImageUrl: String?,
        thumbnailUrl: String?,
        playCount: Int?,
        url: String,
        albumId: UUID?,
        wiki: LastFmWiki?,
        tags: List<String>,
    ) : this(
            musicBrainzId = musicBrainzId,
            artist = artist,
            name = name,
            tracks = emptyList(),
            listeners = listeners,
            fullImageUrl = fullImageUrl,
            thumbnailUrl = thumbnailUrl,
            playCount = playCount,
            url = url,
            albumId = albumId,
            wiki = wiki,
            tags = tags,
        )

    val year: Int?
        /** A tag consisting only of 4 digits is probably a year. */
        get() = tags.firstOrNull { it.matches(Regex("^\\d{4}$")) }?.toIntOrNull()
}
