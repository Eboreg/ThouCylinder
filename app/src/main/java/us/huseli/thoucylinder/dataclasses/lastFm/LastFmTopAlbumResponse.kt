package us.huseli.thoucylinder.dataclasses.lastFm

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import us.huseli.thoucylinder.asThumbnailImageBitmap
import us.huseli.thoucylinder.getBitmapByUrl
import us.huseli.thoucylinder.interfaces.IExternalAlbum

data class LastFmTopAlbumsResponse(val topalbums: TopAlbums) {
    data class TopAlbums(val album: List<Album>)

    data class Album(
        val mbid: String,
        val url: String,
        val name: String,
        val artist: Artist,
        val image: List<LastFmImage>,
        val playcount: String?,
    ) : IExternalAlbum {
        override val id: String
            get() = mbid
        override val title: String
            get() = name
        override val artistName: String
            get() = artist.name

        override suspend fun getThumbnailImageBitmap(context: Context): ImageBitmap? =
            image.getThumbnail()?.let { it.url.getBitmapByUrl()?.asThumbnailImageBitmap(context) }

        override fun toString(): String = artistName.takeIf { it.isNotEmpty() }?.let { "$it - $title" } ?: title
    }

    data class Artist(
        val url: String,
        val name: String,
        val mbid: String,
    )
}

fun List<LastFmTopAlbumsResponse.Album>.filterBySearchTerm(term: String): List<LastFmTopAlbumsResponse.Album> {
    val words = term.lowercase().split(Regex(" +"))

    return filter { album ->
        words.all { album.artist.name.lowercase().contains(it) || album.name.lowercase().contains(it) }
    }
}
