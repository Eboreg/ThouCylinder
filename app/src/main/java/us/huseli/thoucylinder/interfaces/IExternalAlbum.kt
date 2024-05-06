package us.huseli.thoucylinder.interfaces

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toUri
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.UnsavedArtist
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.toMediaStoreImage
import us.huseli.thoucylinder.getCachedThumbnailBitmap
import kotlin.time.Duration

interface IExternalAlbum {
    val id: String
    val title: String
    val artistName: String?
    val thumbnailUrl: String?
    val trackCount: Int?
    val year: Int?
    val duration: Duration?
    val playCount: Int?

    suspend fun getMediaStoreImage(): MediaStoreImage? = thumbnailUrl?.toMediaStoreImage()

    suspend fun getThumbnailImageBitmap(context: Context): ImageBitmap? =
        thumbnailUrl?.toUri()?.getCachedThumbnailBitmap(context)?.asImageBitmap()

    suspend fun toAlbumWithTracks(
        isLocal: Boolean,
        isInLibrary: Boolean,
        getArtist: suspend (UnsavedArtist) -> Artist,
    ): AlbumWithTracksCombo
}

fun <A : IExternalAlbum> Iterable<A>.filterBySearchTerm(term: String): List<A> {
    val words = term.lowercase().split(Regex(" +"))

    return filterNotNull().filter { album ->
        words.all {
            album.artistName?.lowercase()?.contains(it) == true ||
                album.title.lowercase().contains(it) ||
                album.year?.toString()?.contains(it) == true
        }
    }
}
