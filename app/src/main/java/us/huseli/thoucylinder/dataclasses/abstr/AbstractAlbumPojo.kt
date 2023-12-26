package us.huseli.thoucylinder.dataclasses.abstr

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.scaleToMaxSize
import us.huseli.thoucylinder.Constants.IMAGE_MAX_DP_FULL
import us.huseli.thoucylinder.Constants.IMAGE_MAX_DP_THUMBNAIL
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.dataclasses.entities.LastFmAlbum
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbum
import us.huseli.thoucylinder.dataclasses.entities.Style
import us.huseli.thoucylinder.getBitmapByUrl
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

abstract class AbstractAlbumPojo {
    abstract val album: Album
    abstract val genres: List<Genre>
    abstract val styles: List<Style>
    abstract val trackCount: Int
    abstract val durationMs: Long?
    abstract val minYear: Int?
    abstract val maxYear: Int?
    abstract val spotifyAlbum: SpotifyAlbum?
    abstract val isPartiallyDownloaded: Boolean
    abstract val lastFmAlbum: LastFmAlbum?

    val duration: Duration?
        get() = durationMs?.milliseconds

    val isOnSpotify: Boolean
        get() = spotifyAlbum != null

    val lastFmWebUrl: String?
        get() = lastFmAlbum?.url

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

    suspend fun getFullImage(context: Context): ImageBitmap? =
        getFullImageBitmap(context)?.scaleToMaxSize(IMAGE_MAX_DP_FULL.dp, context)?.asImageBitmap()

    suspend fun getThumbnail(context: Context): ImageBitmap? =
        getThumbnailBitmap(context)?.scaleToMaxSize(IMAGE_MAX_DP_THUMBNAIL.dp, context)?.asImageBitmap()

    private suspend fun getFullImageBitmap(context: Context): Bitmap? =
        album.albumArt?.getFullImageBitmap(context)
            ?: spotifyAlbum?.fullImage?.url?.getBitmapByUrl()
            ?: lastFmAlbum?.fullImageUrl?.getBitmapByUrl()
            ?: album.youtubePlaylist?.fullImage?.url?.getBitmapByUrl()

    private suspend fun getThumbnailBitmap(context: Context): Bitmap? =
        album.albumArt?.getThumbnailBitmap(context)
            ?: spotifyAlbum?.thumbnail?.url?.getBitmapByUrl()
            ?: spotifyAlbum?.fullImage?.url?.getBitmapByUrl()
            ?: lastFmAlbum?.thumbnailUrl?.getBitmapByUrl()
            ?: album.youtubePlaylist?.thumbnail?.url?.getBitmapByUrl()
}
