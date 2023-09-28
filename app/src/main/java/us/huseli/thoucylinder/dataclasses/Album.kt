package us.huseli.thoucylinder.dataclasses

import android.os.Environment
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import us.huseli.retaintheme.sanitizeFilename
import us.huseli.thoucylinder.sum
import java.io.File
import java.util.UUID
import kotlin.time.Duration

@Entity
data class Album(
    @PrimaryKey val albumId: UUID = UUID.randomUUID(),
    val title: String,
    val isInLibrary: Boolean,
    val artist: String? = null,
    val year: Int? = null,
    @Embedded("youtubePlaylist") val youtubePlaylist: YoutubePlaylist? = null,
    @Embedded("albumArt") val albumArt: Image? = null,
    @Embedded("local") val local: LocalAlbumData? = null,
    @Ignore val tracks: List<Track> = emptyList(),
    @Ignore val genres: List<String> = emptyList(),
    @Ignore val styles: List<String> = emptyList(),
) {
    constructor(
        albumId: UUID,
        title: String,
        isInLibrary: Boolean,
        artist: String?,
        year: Int?,
        youtubePlaylist: YoutubePlaylist?,
        albumArt: Image?,
        local: LocalAlbumData?,
    ) : this(
        albumId,
        title,
        isInLibrary,
        artist,
        year,
        youtubePlaylist,
        albumArt,
        local,
        emptyList(),
        emptyList(),
        emptyList(),
    )

    private val years: Pair<Int, Int>?
        get() = year?.let { Pair(it, it) }
            ?: tracks.mapNotNull { it.year }.takeIf { it.isNotEmpty() }?.let { Pair(it.min(), it.max()) }

    val duration: Duration
        get() = tracks.mapNotNull { it.metadata?.duration }.sum()

    val isLocal: Boolean
        get() = local != null

    val isOnYoutube: Boolean
        get() = youtubePlaylist != null

    val yearString: String?
        get() = years?.let { (min, max) ->
            if (min == max) min.toString()
            else "$min–$max"
        }

    private fun generateMediaStorePath(): String = (artist?.let { "$artist - $title" } ?: title).sanitizeFilename()

    fun getMediaStorePath(): String = local?.mediaStorePath ?: generateMediaStorePath()

    override fun toString(): String = artist?.let { "$it - $title" } ?: title
}


data class LocalAlbumData(
    val mediaStorePath: String,
) {
    val mediaStoreDir: File?
        get() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), mediaStorePath)
            .takeIf { it.isDirectory }
}
