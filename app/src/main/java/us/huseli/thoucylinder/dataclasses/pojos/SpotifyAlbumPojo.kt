package us.huseli.thoucylinder.dataclasses.pojos

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbum
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbumArtist
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbumGenre
import us.huseli.thoucylinder.dataclasses.entities.SpotifyArtist
import us.huseli.thoucylinder.dataclasses.entities.SpotifyTrack
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class SpotifyAlbumPojo(
    @Embedded val spotifyAlbum: SpotifyAlbum,
    @Relation(
        entity = SpotifyArtist::class,
        parentColumn = "SpotifyAlbum_id",
        entityColumn = "SpotifyArtist_id",
        associateBy = Junction(
            value = SpotifyAlbumArtist::class,
            parentColumn = "SpotifyAlbumArtist_albumId",
            entityColumn = "SpotifyAlbumArtist_artistId",
        )
    )
    val artists: List<SpotifyArtist>,
    @Relation(
        entity = Genre::class,
        parentColumn = "SpotifyAlbum_id",
        entityColumn = "Genre_genreName",
        associateBy = Junction(
            value = SpotifyAlbumGenre::class,
            parentColumn = "SpotifyAlbumGenre_albumId",
            entityColumn = "SpotifyAlbumGenre_genreName",
        )
    )
    val genres: List<Genre>,
    @Relation(
        entity = SpotifyTrack::class,
        parentColumn = "SpotifyAlbum_id",
        entityColumn = "SpotifyTrack_spotifyAlbumId",
    )
    val spotifyTrackPojos: List<SpotifyTrackPojo>,
) {
    val artist: String?
        get() = artists.map { it.name }.takeIf { it.isNotEmpty() }?.joinToString("/")

    val durationMs: Int
        get() = spotifyTrackPojos.sumOf { it.track.durationMs }

    val duration: Duration
        get() = durationMs.milliseconds

    private val primaryArtist: String?
        get() = artists.firstOrNull()?.name

    val searchQuery: String
        get() = primaryArtist?.let { "$it - ${spotifyAlbum.name}" } ?: spotifyAlbum.name

    fun toAlbumPojo(isInLibrary: Boolean): AlbumWithTracksPojo {
        val album = toAlbum(isInLibrary)

        return AlbumWithTracksPojo(
            album = album,
            genres = genres,
            tracks = spotifyTrackPojos.map { it.toTrack(isInLibrary = isInLibrary, albumId = album.albumId) },
            spotifyAlbum = spotifyAlbum,
        )
    }

    private fun toAlbum(isInLibrary: Boolean) = Album(
        title = spotifyAlbum.name,
        isLocal = false,
        isInLibrary = isInLibrary,
        artist = artist,
        year = spotifyAlbum.year,
    )
}
