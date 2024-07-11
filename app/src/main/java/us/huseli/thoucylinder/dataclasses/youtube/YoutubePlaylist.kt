package us.huseli.thoucylinder.dataclasses.youtube

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import kotlinx.parcelize.Parcelize
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbum
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbumCombo
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.artist.UnsavedAlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.toMediaStoreImage
import us.huseli.thoucylinder.enums.AlbumType
import us.huseli.thoucylinder.interfaces.IExternalAlbumWithTracks
import java.util.UUID
import kotlin.time.Duration

@Parcelize
@Immutable
data class YoutubePlaylist(
    override val id: String,
    override val title: String,
    val artist: String? = null,
    @Embedded("thumbnail_") val thumbnail: YoutubeImage? = null,
    @Embedded("fullImage_") val fullImage: YoutubeImage? = null,
    val videoCount: Int = 0,
) : Parcelable, IExternalAlbumWithTracks {
    override val albumType: AlbumType?
        get() = if (artist?.lowercase() == "various artists") AlbumType.COMPILATION else null

    override val artistName: String?
        get() = artist

    override val thumbnailUrl: String?
        get() = fullImage?.url ?: thumbnail?.url

    override val trackCount: Int
        get() = videoCount

    override val year: Int?
        get() = null

    override val duration: Duration?
        get() = null

    override val playCount: Int?
        get() = null

    override fun getMediaStoreImage(): MediaStoreImage? =
        (fullImage?.url ?: thumbnail?.url)?.toMediaStoreImage(thumbnail?.url)

    override fun toAlbumCombo(
        isLocal: Boolean,
        isInLibrary: Boolean,
        albumId: String?,
    ): UnsavedAlbumCombo {
        val album = toAlbum(isInLibrary = isInLibrary, isLocal = isLocal, albumId = albumId)
        val albumArtists = artist
            ?.takeIf { it.lowercase() != "various artists" }
            ?.let { UnsavedAlbumArtistCredit(name = it, albumId = album.albumId) }
            ?.let { listOf(it) }
            ?: emptyList()

        return UnsavedAlbumCombo(album = album, artists = albumArtists, isDownloadable = true)
    }

    override fun toAlbumWithTracks(
        isLocal: Boolean,
        isInLibrary: Boolean,
        albumId: String?,
    ): UnsavedAlbumWithTracksCombo {
        val combo = toAlbumCombo(isLocal = isLocal, isInLibrary = isInLibrary)
        return UnsavedAlbumWithTracksCombo(album = combo.album, artists = combo.artists)
    }

    override fun toString() = "${artist?.let { "$it - $title" } ?: title} ($videoCount videos)"

    private fun toAlbum(isInLibrary: Boolean, isLocal: Boolean, albumId: String? = null) = UnsavedAlbum(
        albumArt = getMediaStoreImage(),
        albumId = albumId ?: UUID.randomUUID().toString(),
        albumType = albumType,
        isInLibrary = isInLibrary,
        isLocal = isLocal,
        title = title,
        trackCount = trackCount,
        youtubePlaylist = this,
    )
}
