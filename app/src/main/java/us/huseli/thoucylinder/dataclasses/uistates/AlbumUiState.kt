package us.huseli.thoucylinder.dataclasses.uistates

import android.net.Uri
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import us.huseli.thoucylinder.AlbumDownloadTask
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.views.AlbumArtistCredit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Immutable
data class AlbumUiState(
    val albumId: String,
    val artists: ImmutableList<AlbumArtistCredit>,
    val downloadState: StateFlow<AlbumDownloadTask.UiState?> = MutableStateFlow(null),
    val duration: Duration?,
    val fullImageUri: Uri?,
    val isDeleted: Boolean,
    val isInLibrary: Boolean,
    val isLocal: Boolean,
    val isOnYoutube: Boolean,
    val isPartiallyDownloaded: Boolean,
    val spotifyWebUrl: String?,
    val thumbnailUri: Uri?,
    val title: String,
    val trackCount: Int,
    val unplayableTrackCount: Int,
    val yearString: String?,
    val youtubePlaylistId: String?,
    val youtubeWebUrl: String?,
) {
    val artistString: String?
        get() = artists.joined()

    val isPlayable: Boolean
        get() = isLocal || youtubePlaylistId != null

    companion object {
        fun fromAlbumCombo(combo: AbstractAlbumCombo) = AlbumUiState(
            albumId = combo.album.albumId,
            artists = combo.artists.toImmutableList(),
            duration = combo.durationMs?.milliseconds,
            fullImageUri = combo.album.albumArt?.fullUri,
            isDeleted = combo.album.isDeleted,
            isInLibrary = combo.album.isInLibrary,
            isLocal = combo.album.isLocal,
            isOnYoutube = combo.album.youtubePlaylist != null,
            isPartiallyDownloaded = combo.isPartiallyDownloaded,
            spotifyWebUrl = combo.album.spotifyId?.let { "https://open.spotify.com/album/${it}" },
            thumbnailUri = combo.album.albumArt?.thumbnailUri,
            title = combo.album.title,
            trackCount = combo.trackCount,
            unplayableTrackCount = combo.unplayableTrackCount,
            yearString = combo.yearString,
            youtubePlaylistId = combo.album.youtubePlaylist?.id,
            youtubeWebUrl = combo.album.youtubePlaylist?.let { "https://youtube.com/playlist?list=${it.id}" },
        )
    }
}
