package us.huseli.thoucylinder.dataclasses

import android.content.ContentValues
import android.provider.MediaStore
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import us.huseli.retaintheme.sanitizeFilename
import us.huseli.thoucylinder.sum
import java.util.UUID
import kotlin.math.max
import kotlin.time.Duration

@Entity
data class Album(
    @PrimaryKey val albumId: UUID = UUID.randomUUID(),
    val title: String,
    val isInLibrary: Boolean,
    val isLocal: Boolean,
    val artist: String? = null,
    val year: Int? = null,
    @Embedded("youtubePlaylist") val youtubePlaylist: YoutubePlaylist? = null,
    @Embedded("albumArt") val albumArt: Image? = null,
    @Ignore val tracks: List<Track> = emptyList(),
    @Ignore val genres: List<String> = emptyList(),
    @Ignore val styles: List<String> = emptyList(),
) {
    constructor(
        albumId: UUID,
        title: String,
        isInLibrary: Boolean,
        isLocal: Boolean,
        artist: String?,
        year: Int?,
        youtubePlaylist: YoutubePlaylist?,
        albumArt: Image?,
    ) : this(
        albumId = albumId,
        title = title,
        isInLibrary = isInLibrary,
        isLocal = isLocal,
        artist = artist,
        year = year,
        youtubePlaylist = youtubePlaylist,
        albumArt = albumArt,
        tracks = emptyList(),
        genres = emptyList(),
        styles = emptyList(),
    )

    private val years: Pair<Int, Int>?
        get() = year?.let { Pair(it, it) }
            ?: tracks.mapNotNull { it.year }.takeIf { it.isNotEmpty() }?.let { Pair(it.min(), it.max()) }

    val duration: Duration?
        get() = tracks.mapNotNull { it.metadata?.duration }.sum().takeIf { it > Duration.ZERO }

    val isOnYoutube: Boolean
        get() = youtubePlaylist != null

    val trackCount: Int
        get() = max(youtubePlaylist?.videoCount ?: 0, tracks.size)

    val yearString: String?
        get() = years?.let { (min, max) ->
            if (min == max) min.toString()
            else "$min–$max"
        }

    fun getMediaStoreSubdir(): String =
        artist?.let { "${artist.sanitizeFilename()}/${title.sanitizeFilename()}" } ?: title.sanitizeFilename()

    fun getContentValues() = ContentValues().apply {
        put(MediaStore.Audio.Media.ALBUM, title)
        artist?.also {
            put(MediaStore.Audio.Media.ARTIST, it)
            put(MediaStore.Audio.Media.ALBUM_ARTIST, it)
        }
        year?.also { put(MediaStore.Audio.Media.YEAR, it) }
    }

    override fun toString(): String = artist?.let { "$it - $title" } ?: title
}
