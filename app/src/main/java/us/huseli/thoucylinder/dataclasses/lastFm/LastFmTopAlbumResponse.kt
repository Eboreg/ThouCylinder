package us.huseli.thoucylinder.dataclasses.lastFm

import com.google.gson.reflect.TypeToken
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.getObject

data class LastFmTopAlbumsResponse(val topalbums: TopAlbums) {
    data class TopAlbums(val album: List<Album>) {
        data class Album(
            val mbid: String,
            val url: String,
            val name: String,
            val artist: LastFmArtist,
            val image: List<LastFmImage>,
            val playcount: String?,
        )
    }
}

suspend fun Request.getLastFmTopAlbums(): List<LastFmTopAlbumsResponse.TopAlbums.Album>? =
    connect().getObject(object : TypeToken<LastFmTopAlbumsResponse>() {})
        ?.topalbums?.album?.filter { it.mbid.isNotEmpty() }

fun List<LastFmTopAlbumsResponse.TopAlbums.Album>.filterBySearchTerm(term: String): List<LastFmTopAlbumsResponse.TopAlbums.Album> {
    val words = term.lowercase().split(Regex(" +"))

    return filter { album ->
        words.all { album.artist.name.lowercase().contains(it) || album.name.lowercase().contains(it) }
    }
}
