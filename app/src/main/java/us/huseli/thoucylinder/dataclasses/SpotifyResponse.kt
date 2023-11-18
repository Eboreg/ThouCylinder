@file:Suppress("PropertyName")

package us.huseli.thoucylinder.dataclasses

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import us.huseli.thoucylinder.Constants.IMAGE_MIN_PX_THUMBNAIL
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbum
import us.huseli.thoucylinder.dataclasses.entities.SpotifyArtist
import us.huseli.thoucylinder.dataclasses.entities.SpotifyTrack
import us.huseli.thoucylinder.dataclasses.pojos.SpotifyAlbumPojo
import us.huseli.thoucylinder.dataclasses.pojos.SpotifyTrackPojo

data class SpotifyResponse<T>(
    val href: String,
    val limit: Int,
    val next: String?,
    val offset: Int,
    val previous: String?,
    val total: Int,
    val items: List<T>,
)

data class SpotifyResponseTrack(
    val disc_number: Int,
    val duration_ms: Int,
    val href: String,
    val id: String,
    val name: String,
    val track_number: Int,
    val uri: String,
    val artists: List<SpotifyArtist>,
) {
    fun toSpotifyTrackPojo(albumId: String? = null) = SpotifyTrackPojo(
        track = toSpotifyTrack(albumId),
        artists = artists,
    )

    private fun toSpotifyTrack(albumId: String? = null) = SpotifyTrack(
        discNumber = disc_number,
        durationMs = duration_ms,
        href = href,
        id = id,
        name = name,
        trackNumber = track_number,
        uri = uri,
        albumId = albumId,
        artists = artists.map { it.name },
    )
}

data class SpotifyResponseAlbum(
    val album_type: String,
    val total_tracks: Int,
    val href: String,
    val id: String,
    val name: String,
    val release_date: String,
    val release_date_precision: String,
    val uri: String,
    val images: List<SpotifyAlbumArt>,
    val artists: List<SpotifyArtist>,
    val genres: List<String>,
    val tracks: SpotifyResponse<SpotifyResponseTrack>,
) {
    fun toSpotifyAlbumPojo() = SpotifyAlbumPojo(
        spotifyAlbum = toSpotifyAlbum(),
        artists = artists,
        genres = genres.map { Genre(it) },
        spotifyTrackPojos = tracks.items.map { it.toSpotifyTrackPojo(albumId = id) },
    )

    private fun toSpotifyAlbum() = SpotifyAlbum(
        albumType = album_type,
        totalTracks = total_tracks,
        href = href,
        id = id,
        name = name,
        releaseDate = release_date,
        releaseDatePrecision = release_date_precision,
        uri = uri,
        fullImage = images.maxByOrNull { it.size },
        thumbnail = images.sortedBy { it.size }.firstOrNull { it.width != null && it.width >= IMAGE_MIN_PX_THUMBNAIL },
        artists = artists.map { it.name },
    )
}

data class SpotifyResponseAlbumItem(
    val added_at: String,
    val album: SpotifyResponseAlbum,
) {
    fun toSpotifyAlbumPojo() = album.toSpotifyAlbumPojo()
}

suspend fun Request.getSpotifyAlbums(): SpotifyResponse<SpotifyResponseAlbumItem>? {
    val responseType = object : TypeToken<SpotifyResponse<SpotifyResponseAlbumItem>>() {}
    val gson: Gson = GsonBuilder().create()
    return gson.fromJson(getString(), responseType)
}
