package us.huseli.thoucylinder.dataclasses.abstr

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.SpotifyTrack
import us.huseli.thoucylinder.dataclasses.entities.Track

abstract class AbstractTrackCombo {
    abstract val track: Track
    abstract val album: Album?
    abstract val spotifyTrack: SpotifyTrack?

    val artist: String?
        get() = track.artist ?: album?.artist

    val spotifyWebUrl: String?
        get() = spotifyTrack?.let { "https://open.spotify.com/track/${it.id}" }

    val isOnSpotify: Boolean
        get() = spotifyTrack != null

    suspend fun getFullBitmap(context: Context) =
        track.image?.getFullBitmap(context) ?: album?.albumArt?.getFullBitmap(context)

    open suspend fun getFullImageBitmap(context: Context): ImageBitmap? =
        track.image?.getFullImageBitmap(context) ?: album?.albumArt?.getFullImageBitmap(context)

    override fun equals(other: Any?) = other is AbstractTrackCombo && other.track == track && other.album == album

    override fun hashCode(): Int = 31 * track.hashCode() + (album?.hashCode() ?: 0)
}


fun Collection<AbstractTrackCombo>.tracks(): List<Track> = map { it.track }
