package us.huseli.thoucylinder.dataclasses.views

import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Embedded
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.abstr.AbstractArtistCredit
import us.huseli.thoucylinder.dataclasses.entities.AlbumArtist
import us.huseli.thoucylinder.dataclasses.entities.Artist
import java.util.UUID

@DatabaseView(
    """
    SELECT AlbumArtist.*, Artist_name AS AlbumArtist_name, Artist_spotifyId AS AlbumArtist_spotifyId,
        Artist_musicBrainzId AS AlbumArtist_musicBrainzId, Artist_image_uri AS AlbumArtist_image_uri,
        Artist_image_hash AS AlbumArtist_image_hash, Artist_image_thumbnailUri AS AlbumArtist_image_thumbnailUri
    FROM AlbumArtist JOIN Artist ON AlbumArtist_artistId = Artist_id
    ORDER BY AlbumArtist_position
    """
)
data class AlbumArtistCredit(
    @ColumnInfo("AlbumArtist_albumId") val albumId: UUID,
    @ColumnInfo("AlbumArtist_artistId") override val artistId: UUID = UUID.randomUUID(),
    @ColumnInfo("AlbumArtist_name") override val name: String,
    @ColumnInfo("AlbumArtist_spotifyId") override val spotifyId: String? = null,
    @ColumnInfo("AlbumArtist_musicBrainzId") override val musicBrainzId: String? = null,
    @ColumnInfo("AlbumArtist_joinPhrase") override val joinPhrase: String = "/",
    @Embedded("AlbumArtist_image_") override val image: MediaStoreImage? = null,
    @ColumnInfo("AlbumArtist_position") override val position: Int = 0,
) : AbstractArtistCredit() {
    constructor(artist: Artist, albumId: UUID, joinPhrase: String = "/", position: Int = 0) : this(
        albumId = albumId,
        artistId = artist.id,
        name = artist.name,
        spotifyId = artist.spotifyId,
        musicBrainzId = artist.musicBrainzId,
        image = artist.image,
        joinPhrase = joinPhrase,
        position = position,
    )

    constructor(artist: Artist, albumArtist: AlbumArtist) : this(
        artist = artist,
        albumId = albumArtist.albumId,
        joinPhrase = albumArtist.joinPhrase,
        position = albumArtist.position,
    )

    fun toAlbumArtist() =
        AlbumArtist(albumId = albumId, artistId = artistId, joinPhrase = joinPhrase, position = position)
}

fun Iterable<AlbumArtistCredit>.toAlbumArtists(): List<AlbumArtist> = map { it.toAlbumArtist() }
