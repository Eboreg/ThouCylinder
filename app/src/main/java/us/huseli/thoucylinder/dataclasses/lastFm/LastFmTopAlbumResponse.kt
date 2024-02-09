package us.huseli.thoucylinder.dataclasses.lastFm

import com.google.gson.reflect.TypeToken
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.getObject

data class LastFmTopAlbumsResponse(val topalbums: TopAlbums) {
    data class TopAlbums(val album: List<Album>)

    data class Album(
        val mbid: String,
        val url: String,
        val name: String,
        val artist: Artist,
        val image: List<LastFmImage>,
        val playcount: String?,
    )

    data class Artist(
        val url: String,
        val name: String,
        val mbid: String,
    )
}

suspend fun Request.getLastFmTopAlbums(): List<LastFmTopAlbumsResponse.Album>? =
    connect().getObject(object : TypeToken<LastFmTopAlbumsResponse>() {})
        ?.topalbums?.album?.filter { it.mbid.isNotEmpty() }

fun List<LastFmTopAlbumsResponse.Album>.filterBySearchTerm(term: String): List<LastFmTopAlbumsResponse.Album> {
    val words = term.lowercase().split(Regex(" +"))

    return filter { album ->
        words.all { album.artist.name.lowercase().contains(it) || album.name.lowercase().contains(it) }
    }
}
