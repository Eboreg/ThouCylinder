package us.huseli.thoucylinder.dataclasses.lastfm

import com.google.gson.annotations.SerializedName
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbum
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbumWithTracksCombo
import us.huseli.thoucylinder.enums.AlbumType
import us.huseli.thoucylinder.interfaces.IExternalAlbumWithTracks
import java.util.UUID
import kotlin.time.Duration

data class LastFmAlbum(
    val mbid: String,
    val url: String,
    val name: String,
    val artist: LastFmArtist,
    val image: List<LastFmImage>,
    @SerializedName("playcount") val playCountString: String?,
) : IExternalAlbumWithTracks {
    override val albumType: AlbumType?
        get() = if (artist.name.lowercase() == "various artists") AlbumType.COMPILATION else null

    override val artistName: String
        get() = artist.name

    override val duration: Duration?
        get() = null

    override val id: String
        get() = mbid

    override val playCount: Int?
        get() = playCountString?.toInt()

    override val thumbnailUrl: String?
        get() = image.getThumbnail()?.url

    override val title: String
        get() = name

    override val trackCount: Int?
        get() = null

    override val year: Int?
        get() = null

    override fun getMediaStoreImage(): MediaStoreImage? = image.toMediaStoreImage()

    override fun toAlbumWithTracks(
        isLocal: Boolean,
        isInLibrary: Boolean,
        albumId: String?,
    ): UnsavedAlbumWithTracksCombo {
        val album = UnsavedAlbum(
            albumArt = getMediaStoreImage(),
            albumId = albumId ?: UUID.randomUUID().toString(),
            albumType = albumType,
            isInLibrary = isInLibrary,
            isLocal = isLocal,
            musicBrainzReleaseId = mbid,
            title = title,
        )

        return UnsavedAlbumWithTracksCombo(
            album = album,
            artists = listOf(artist.toNativeAlbumArtist(albumId = album.albumId)),
        )
    }

    override fun toString(): String = artistName.takeIf { it.isNotEmpty() }?.let { "$it - $title" } ?: title
}
