package us.huseli.thoucylinder.dataclasses.track

import androidx.compose.runtime.Immutable
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.Relation
import us.huseli.thoucylinder.dataclasses.album.Album
import us.huseli.thoucylinder.dataclasses.artist.AlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.TrackArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.joined
import us.huseli.thoucylinder.interfaces.ILogger
import us.huseli.thoucylinder.umlautify
import java.util.UUID

@DatabaseView(
    """
    SELECT
        TrackCombo.*,
        COALESCE(Track_localUri, Track_youtubeVideo_metadata_url) AS QueueTrackCombo_uri,
        QueueTrack_queueTrackId,
        QueueTrack_position
    FROM QueueTrack JOIN TrackCombo ON Track_trackId = QueueTrack_trackId
    GROUP BY QueueTrack_queueTrackId
    HAVING QueueTrackCombo_uri IS NOT NULL
    ORDER BY QueueTrack_position, QueueTrack_queueTrackId
    """
)
@Immutable
data class QueueTrackCombo(
    @Embedded override val track: Track,
    @Embedded override val album: Album?,
    @ColumnInfo("QueueTrackCombo_uri") val uri: String,
    @ColumnInfo("QueueTrack_queueTrackId") val queueTrackId: String = UUID.randomUUID().toString(),
    @ColumnInfo("QueueTrack_position") val position: Int = 0,
    @Relation(parentColumn = "Track_trackId", entityColumn = "TrackArtist_trackId", entity = TrackArtistCredit::class)
    override val trackArtists: List<TrackArtistCredit>,
    @Relation(parentColumn = "Track_albumId", entityColumn = "AlbumArtist_albumId", entity = AlbumArtistCredit::class)
    override val albumArtists: List<AlbumArtistCredit> = emptyList(),
) : ISavedTrackCombo, ILogger {
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

    override fun toUiState(isSelected: Boolean): TrackUiState =
        super.toUiState(isSelected = isSelected).copy(id = queueTrackId)

    override fun withTrack(track: Track) = copy(track = track)

    private fun getMediaMetadata(): MediaMetadata {
        return MediaMetadata.Builder()
            .setArtist(artistString?.umlautify())
            .setTitle(track.title.umlautify())
            .setAlbumArtist(albumArtists.joined()?.umlautify())
            .setAlbumTitle(album?.title?.umlautify())
            .setDiscNumber(track.discNumber)
            .setReleaseYear(track.year ?: album?.year)
            .setArtworkUri(album?.albumArt?.fullUri ?: track.image?.fullUri)
            .build()
    }

    override fun equals(other: Any?) = other is QueueTrackCombo &&
        other.track.trackId == track.trackId &&
        other.queueTrackId == queueTrackId &&
        other.uri == uri &&
        other.position == position

    override fun hashCode(): Int {
        var result = track.trackId.hashCode()
        result = 31 * result + uri.hashCode()
        result = 31 * result + queueTrackId.hashCode()
        result = 31 * result + position
        return result
    }
}

fun Iterable<QueueTrackCombo>.reindexed(offset: Int = 0): List<QueueTrackCombo> =
    mapIndexed { index, combo -> combo.copy(position = index + offset) }

fun Iterable<QueueTrackCombo>.plus(item: QueueTrackCombo, index: Int): List<QueueTrackCombo> =
    toMutableList().apply { add(index, item) }.toList()

fun Iterable<QueueTrackCombo>.containsWithPosition(other: QueueTrackCombo): Boolean =
    any { it == other && it.position == other.position }
