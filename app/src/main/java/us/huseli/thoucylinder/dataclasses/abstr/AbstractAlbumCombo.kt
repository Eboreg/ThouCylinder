package us.huseli.thoucylinder.dataclasses.abstr

import android.content.Context
import androidx.annotation.WorkerThread
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbum
import us.huseli.thoucylinder.dataclasses.entities.Style
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

abstract class AbstractAlbumCombo {
    abstract val album: Album
    abstract val genres: List<Genre>
    abstract val styles: List<Style>
    abstract val trackCount: Int
    abstract val durationMs: Long?
    abstract val minYear: Int?
    abstract val maxYear: Int?
    abstract val spotifyAlbum: SpotifyAlbum?
    abstract val isPartiallyDownloaded: Boolean

    val duration: Duration?
        get() = durationMs?.milliseconds

    val isOnSpotify: Boolean
        get() = spotifyAlbum != null

    val spotifyWebUrl: String?
        get() = spotifyAlbum?.let { "https://open.spotify.com/album/${it.id}" }

    private val years: Pair<Int, Int>?
        get() {
            val year = this.album.year?.takeIf { it > 1000 }
            val minYear = this.minYear?.takeIf { it > 1000 }
            val maxYear = this.maxYear?.takeIf { it > 1000 }

            return if (year != null) Pair(year, year)
            else if (minYear != null && maxYear != null) Pair(minYear, maxYear)
            else null
        }

    val yearString: String?
        get() = years?.let { (min, max) ->
            if (min == max) min.toString()
            else "$min–$max"
        }

    @WorkerThread
    suspend fun getOrCreateAlbumArt(context: Context): MediaStoreImage? {
        val imageUrl = spotifyAlbum?.fullImage?.url ?: album.youtubePlaylist?.fullImage?.url

        return album.albumArt ?: imageUrl?.let { album.saveInternalAlbumArtFiles(it, context) }
    }
}
