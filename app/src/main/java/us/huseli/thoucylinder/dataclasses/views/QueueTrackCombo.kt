package us.huseli.thoucylinder.dataclasses.views

import androidx.compose.runtime.Immutable
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.Relation
import us.huseli.thoucylinder.interfaces.ILogger
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.QueueTrack
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.umlautify
import java.util.UUID

@DatabaseView(
    """
    SELECT
        TrackCombo.*,
        COALESCE(Track_localUri, Track_youtubeVideo_metadata_url) AS uri,
        QueueTrack_queueTrackId,
        QueueTrack_position
    FROM QueueTrack JOIN TrackCombo ON Track_trackId = QueueTrack_trackId
    GROUP BY QueueTrack_queueTrackId
    HAVING uri IS NOT NULL
    ORDER BY QueueTrack_position, QueueTrack_queueTrackId
    """
)
@Immutable
data class QueueTrackCombo(
    @Embedded override val track: Track,
    @Embedded override val album: Album?,
    // @ColumnInfo("QueueTrack_uri") val uri: String,
    val uri: String,
    @ColumnInfo("QueueTrack_queueTrackId") val queueTrackId: String = UUID.randomUUID().toString(),
    @ColumnInfo("QueueTrack_position") val position: Int = 0,
    @Relation(parentColumn = "Track_trackId", entityColumn = "TrackArtist_trackId")
    override val artists: List<TrackArtistCredit>,
    @Relation(parentColumn = "Track_albumId", entityColumn = "AlbumArtist_albumId")
    override val albumArtists: List<AlbumArtistCredit> = emptyList(),
) : AbstractTrackCombo(), ILogger {
    val metadataRefreshNeeded: Boolean
        get() = track.youtubeVideo?.metadataRefreshNeeded == true

    val queueTrack: QueueTrack
        get() = QueueTrack(queueTrackId = queueTrackId, trackId = track.trackId, position = position)

    fun toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(queueTrackId)
        .setUri(uri)
        .setMediaMetadata(getMediaMetadata())
        .setTag(this)
        .build()

    private fun getMediaMetadata(): MediaMetadata {
        return MediaMetadata.Builder()
            .setArtist(artists.joined()?.umlautify())
            .setTitle(track.title.umlautify())
            .setAlbumArtist(albumArtists.joined()?.umlautify())
            .setAlbumTitle(album?.title?.umlautify())
            .setDiscNumber(track.discNumber)
            .setReleaseYear(track.year ?: album?.year)
            .setArtworkUri(album?.albumArt?.fullUri)
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

fun List<QueueTrackCombo>.containsWithPosition(other: QueueTrackCombo): Boolean =
    any { it == other && it.position == other.position }
