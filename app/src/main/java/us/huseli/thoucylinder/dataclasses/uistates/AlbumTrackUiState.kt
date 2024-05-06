package us.huseli.thoucylinder.dataclasses.uistates

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow
import us.huseli.thoucylinder.TrackDownloadTask
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit
import kotlin.time.Duration

@Immutable
data class AlbumTrackUiState(
    val downloadState: StateFlow<TrackDownloadTask.UiState?>,
    val duration: Duration? = null,
    val isDownloadable: Boolean,
    val isInLibrary: Boolean,
    val isPlayable: Boolean,
    val positionString: String,
    val spotifyWebUrl: String? = null,
    val title: String,
    val trackArtists: ImmutableList<TrackArtistCredit>,
    val trackId: String,
    val youtubeWebUrl: String? = null,
) {
    val trackArtistString: String?
        get() = trackArtists.joined()
}
