package us.huseli.thoucylinder.dataclasses.artist

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Embedded
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import java.util.UUID

interface IAlbumArtistCredit : IArtistCredit {
    val albumId: String

    fun withAlbumId(albumId: String): IAlbumArtistCredit
    override fun withSpotifyId(spotifyId: String): IAlbumArtistCredit

    fun withArtistId(artistId: String) = AlbumArtistCredit(
        albumId = albumId,
        artistId = artistId,
        name = name,
        spotifyId = spotifyId,
        musicBrainzId = musicBrainzId,
        joinPhrase = joinPhrase,
        image = image,
        position = position,
    )
}

@Immutable
data class UnsavedAlbumArtistCredit(
    override val albumId: String,
    override val name: String,
    override val spotifyId: String? = null,
    override val musicBrainzId: String? = null,
    override val image: MediaStoreImage? = null,
    override val joinPhrase: String = "/",
    override val position: Int = 0,
) : IAlbumArtistCredit {
    override fun withAlbumId(albumId: String): UnsavedAlbumArtistCredit = copy(albumId = albumId)
    override fun withSpotifyId(spotifyId: String): UnsavedAlbumArtistCredit = copy(spotifyId = spotifyId)
}

@DatabaseView(
    """
    SELECT AlbumArtist.*,
        Artist_name AS AlbumArtist_name,
        Artist_spotifyId AS AlbumArtist_spotifyId,
        Artist_musicBrainzId AS AlbumArtist_musicBrainzId,
        Artist_image_fullUriString AS AlbumArtist_image_fullUriString,
        Artist_image_thumbnailUriString AS AlbumArtist_image_thumbnailUriString
    FROM AlbumArtist JOIN Artist ON AlbumArtist_artistId = Artist_id
    ORDER BY AlbumArtist_position
    """
)
@Immutable
data class AlbumArtistCredit(
    @ColumnInfo("AlbumArtist_albumId") override val albumId: String,
    @ColumnInfo("AlbumArtist_artistId") override val artistId: String = UUID.randomUUID().toString(),
    @ColumnInfo("AlbumArtist_name") override val name: String,
    @ColumnInfo("AlbumArtist_spotifyId") override val spotifyId: String? = null,
    @ColumnInfo("AlbumArtist_musicBrainzId") override val musicBrainzId: String? = null,
    @ColumnInfo("AlbumArtist_joinPhrase") override val joinPhrase: String = "/",
    @Embedded("AlbumArtist_image_") override val image: MediaStoreImage? = null,
    @ColumnInfo("AlbumArtist_position") override val position: Int = 0,
) : IAlbumArtistCredit, ISavedArtistCredit {
    override fun withAlbumId(albumId: String): AlbumArtistCredit = copy(albumId = albumId)
    override fun withSpotifyId(spotifyId: String): AlbumArtistCredit = copy(spotifyId = spotifyId)
}

fun Iterable<AlbumArtistCredit>.toAlbumArtists(): List<AlbumArtist> = map {
    AlbumArtist(
        albumId = it.albumId,
        artistId = it.artistId,
        joinPhrase = it.joinPhrase,
        position = it.position,
    )
}
