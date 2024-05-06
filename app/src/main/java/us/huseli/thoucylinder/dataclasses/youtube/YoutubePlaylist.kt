package us.huseli.thoucylinder.dataclasses.youtube

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import kotlinx.parcelize.Parcelize
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.UnsavedArtist
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.toMediaStoreImage
import us.huseli.thoucylinder.dataclasses.views.AlbumArtistCredit
import us.huseli.thoucylinder.interfaces.IExternalAlbum
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
) : Parcelable, IExternalAlbum {
    suspend fun toAlbum(isInLibrary: Boolean, isLocal: Boolean) = Album(
        title = title,
        isInLibrary = isInLibrary,
        isLocal = isLocal,
        youtubePlaylist = this,
        albumArt = getMediaStoreImage(),
    )

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

    override suspend fun getMediaStoreImage(): MediaStoreImage? =
        (fullImage?.url ?: thumbnail?.url)?.toMediaStoreImage(thumbnail?.url)

    override suspend fun toAlbumWithTracks(
        isLocal: Boolean,
        isInLibrary: Boolean,
        getArtist: suspend (UnsavedArtist) -> Artist,
    ): AlbumWithTracksCombo {
        val album = toAlbum(isInLibrary = isInLibrary, isLocal = isLocal)
        val albumArtist =
            artist?.let { AlbumArtistCredit(artist = getArtist(UnsavedArtist(name = it)), albumId = album.albumId) }

        return AlbumWithTracksCombo(album = album, artists = albumArtist?.let { listOf(it) } ?: emptyList())
    }

    override fun toString() = "${artist?.let { "$it - $title" } ?: title} ($videoCount videos)"
}
