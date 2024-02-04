package us.huseli.thoucylinder.dataclasses.pojos

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.room.ColumnInfo
import androidx.room.Embedded
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.LastFmTrack
import us.huseli.thoucylinder.dataclasses.entities.QueueTrack
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbum
import us.huseli.thoucylinder.dataclasses.entities.SpotifyTrack
import us.huseli.thoucylinder.dataclasses.entities.Track
import java.util.UUID

data class QueueTrackPojo(
    @Embedded override val track: Track,
    @Embedded override val album: Album?,
    @Embedded override val spotifyTrack: SpotifyTrack?,
    @Embedded override val lastFmTrack: LastFmTrack?,
    @Embedded val spotifyAlbum: SpotifyAlbum? = null,
    @ColumnInfo("QueueTrack_uri") val uri: Uri,
    @ColumnInfo("QueueTrack_queueTrackId") val queueTrackId: UUID = UUID.randomUUID(),
    @ColumnInfo("QueueTrack_position") val position: Int = 0,
) : AbstractTrackPojo() {
    val queueTrack: QueueTrack
        get() = QueueTrack(queueTrackId = queueTrackId, trackId = track.trackId, uri = uri, position = position)

    fun toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(queueTrackId.toString())
        .setUri(uri)
        .setMediaMetadata(getMediaMetadata())
        .setTag(this)
        .build()

    private fun getMediaMetadata(): MediaMetadata {
        return MediaMetadata.Builder()
            .setArtist(track.artist)
            .setTitle(track.title)
            .setAlbumArtist(album?.artist)
            .setAlbumTitle(album?.title)
            .setDiscNumber(track.discNumber)
            .setReleaseYear(track.year ?: album?.year)
            .setArtworkUri(
                album?.albumArt?.uri
                    ?: album?.lastFmFullImageUrl?.toUri()
                    ?: album?.youtubePlaylist?.fullImage?.url?.toUri()
            )
            .build()
    }

    override fun equals(other: Any?) = other is QueueTrackPojo &&
        other.track.trackId == track.trackId &&
        other.queueTrackId == queueTrackId &&
        other.uri == uri

    override suspend fun getFullImageBitmap(context: Context): ImageBitmap? {
        return super.getFullImageBitmap(context) ?: spotifyAlbum?.getFullImageBitmap(context)
    }

    override fun hashCode(): Int = 31 * (31 * track.trackId.hashCode() + uri.hashCode()) + queueTrackId.hashCode()
}

fun List<QueueTrackPojo>.reindexed(): List<QueueTrackPojo> = mapIndexed { index, pojo -> pojo.copy(position = index) }

fun List<QueueTrackPojo>.plus(item: QueueTrackPojo, index: Int): List<QueueTrackPojo> =
    toMutableList().apply { add(index, item) }.toList()

fun List<QueueTrackPojo>.toMediaItems(): List<MediaItem> = map { it.toMediaItem() }

fun List<QueueTrackPojo>.containsWithPosition(other: QueueTrackPojo): Boolean =
    any { it == other && it.position == other.position }
