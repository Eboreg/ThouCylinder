package us.huseli.thoucylinder.dataclasses.lastFm

import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.UnsavedArtist
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.views.AlbumArtistCredit
import us.huseli.thoucylinder.interfaces.IExternalAlbum
import kotlin.time.Duration

data class LastFmTopAlbumsResponse(val topalbums: TopAlbums) {
    data class TopAlbums(val album: List<LastFmAlbum>)

    data class LastFmAlbum(
        val mbid: String,
        val url: String,
        val name: String,
        val artist: LastFmArtist,
        val image: List<LastFmImage>,
        val playcount: String?,
    ) : IExternalAlbum {
        override val artistName: String
            get() = artist.name

        override val duration: Duration?
            get() = null

        override val id: String
            get() = mbid

        override val playCount: Int?
            get() = playcount?.toInt()

        override val thumbnailUrl: String?
            get() = image.getThumbnail()?.url

        override val title: String
            get() = name

        override val trackCount: Int?
            get() = null

        override val year: Int?
            get() = null

        override suspend fun getMediaStoreImage(): MediaStoreImage? = image.toMediaStoreImage()

        override suspend fun toAlbumWithTracks(
            isLocal: Boolean,
            isInLibrary: Boolean,
            getArtist: suspend (UnsavedArtist) -> Artist,
        ): AlbumWithTracksCombo {
            val album = Album(
                title = title,
                isInLibrary = isInLibrary,
                isLocal = isLocal,
                musicBrainzReleaseId = mbid,
                albumArt = getMediaStoreImage(),
            )

            return AlbumWithTracksCombo(
                album = album,
                artists = listOf(
                    AlbumArtistCredit(
                        artist = getArtist(UnsavedArtist(name = artist.name, musicBrainzId = artist.mbid)),
                        albumId = album.albumId,
                    )
                ),
            )
        }

        override fun toString(): String = artistName.takeIf { it.isNotEmpty() }?.let { "$it - $title" } ?: title
    }

    data class LastFmArtist(
        val url: String,
        val name: String,
        val mbid: String,
    )
}
