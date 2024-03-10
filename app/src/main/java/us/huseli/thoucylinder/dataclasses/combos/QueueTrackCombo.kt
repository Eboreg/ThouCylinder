package us.huseli.thoucylinder.dataclasses.combos

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Relation
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.QueueTrack
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit
import us.huseli.thoucylinder.umlautify
import java.util.UUID

data class QueueTrackCombo(
    @Embedded override val track: Track,
    @Embedded override val album: Album?,
    override val albumArtist: String?,
    @ColumnInfo("QueueTrack_uri") val uri: Uri,
    @ColumnInfo("QueueTrack_queueTrackId") val queueTrackId: UUID = UUID.randomUUID(),
    @ColumnInfo("QueueTrack_position") val position: Int = 0,
    @Relation(parentColumn = "Track_trackId", entityColumn = "TrackArtist_trackId")
    override val artists: List<TrackArtistCredit>,
) : AbstractTrackCombo() {
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
            .setArtist(artists.joined()?.umlautify())
            .setTitle(track.title.umlautify())
            .setAlbumArtist(albumArtist?.umlautify())
            .setAlbumTitle(album?.title?.umlautify())
            .setDiscNumber(track.discNumber)
            .setReleaseYear(track.year ?: album?.year)
            .setArtworkUri(album?.albumArt?.uri)
            .build()
    }

    override fun equals(other: Any?) = other is QueueTrackCombo &&
        other.track.trackId == track.trackId &&
        other.queueTrackId == queueTrackId &&
        other.uri == uri

    override fun hashCode(): Int = 31 * (31 * track.trackId.hashCode() + uri.hashCode()) + queueTrackId.hashCode()
}

fun List<QueueTrackCombo>.reindexed(offset: Int = 0): List<QueueTrackCombo> =
    mapIndexed { index, combo -> combo.copy(position = index + offset) }

fun List<QueueTrackCombo>.plus(item: QueueTrackCombo, index: Int): List<QueueTrackCombo> =
    toMutableList().apply { add(index, item) }.toList()

fun List<QueueTrackCombo>.toMediaItems(): List<MediaItem> = map { it.toMediaItem() }

fun List<QueueTrackCombo>.containsWithPosition(other: QueueTrackCombo): Boolean =
    any { it == other && it.position == other.position }
