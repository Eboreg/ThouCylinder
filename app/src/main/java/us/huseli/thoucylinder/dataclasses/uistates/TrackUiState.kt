package us.huseli.thoucylinder.dataclasses.uistates

import android.net.Uri
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import us.huseli.thoucylinder.TrackDownloadTask
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.views.PlaylistTrackCombo
import us.huseli.thoucylinder.dataclasses.views.QueueTrackCombo
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Immutable
data class TrackUiState(
    val albumFullImageUri: Uri?,
    val albumId: String?,
    val albumPosition: Int?,
    val albumThumbnailUri: Uri?,
    val albumTitle: String?,
    val artistString: String?,
    val discNumber: Int?,
    val downloadState: StateFlow<TrackDownloadTask.UiState?> = MutableStateFlow(null),
    val durationMs: Long?,
    val id: String,
    val isDownloadable: Boolean,
    val isInLibrary: Boolean,
    val isPlayable: Boolean,
    val spotifyWebUrl: String?,
    val title: String,
    val trackArtists: ImmutableList<TrackArtistCredit>,
    val trackFullImageUri: Uri?,
    val trackId: String = id,
    val trackThumbnailUri: Uri?,
    val year: Int?,
    val youtubeWebUrl: String?,
) {
    val duration: Duration?
        get() = durationMs?.milliseconds

    companion object {
        fun fromTrack(track: Track) = TrackUiState(
            albumFullImageUri = null,
            albumId = track.albumId,
            albumPosition = track.albumPosition,
            albumThumbnailUri = null,
            albumTitle = null,
            artistString = null,
            discNumber = track.discNumber,
            durationMs = track.durationMs,
            id = track.trackId,
            isDownloadable = track.isDownloadable,
            isInLibrary = track.isInLibrary,
            isPlayable = track.isPlayable,
            spotifyWebUrl = track.spotifyWebUrl,
            title = track.title,
            trackArtists = persistentListOf(),
            trackFullImageUri = track.image?.fullUri,
            trackThumbnailUri = track.image?.thumbnailUri,
            year = track.year,
            youtubeWebUrl = track.youtubeWebUrl,
        )

        fun fromTrackCombo(combo: AbstractTrackCombo) = fromTrack(combo.track).copy(
            albumFullImageUri = combo.album?.albumArt?.fullUri,
            albumThumbnailUri = combo.album?.albumArt?.thumbnailUri,
            albumTitle = combo.album?.title,
            artistString = combo.artists.joined() ?: combo.albumArtists.joined(),
            trackArtists = combo.artists.toImmutableList(),
        )

        fun fromPlaylistTrackCombo(combo: PlaylistTrackCombo) =
            fromTrackCombo(combo).copy(id = combo.id)

        fun fromQueueTrackCombo(combo: QueueTrackCombo) =
            fromTrackCombo(combo).copy(id = combo.queueTrackId)
    }
}
