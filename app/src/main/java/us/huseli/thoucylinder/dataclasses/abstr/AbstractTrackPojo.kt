package us.huseli.thoucylinder.dataclasses.abstr

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import java.util.UUID

abstract class AbstractTrackPojo {
    abstract val track: Track
    abstract val album: Album?

    val trackId: UUID
        get() = track.trackId

    val artist: String?
        get() = track.artist ?: album?.artist

    suspend fun getFullImage(context: Context): ImageBitmap? =
        track.getFullImage(context) ?: album?.getFullImage(context)

    override fun equals(other: Any?) = other is AbstractTrackPojo && other.track == track && other.album == album

    override fun hashCode(): Int = 31 * track.hashCode() + (album?.hashCode() ?: 0)
}
