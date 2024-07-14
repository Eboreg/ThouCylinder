package us.huseli.thoucylinder.viewmodels

import androidx.compose.ui.graphics.Color
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import us.huseli.thoucylinder.dataclasses.ModalCoverBooleans
import us.huseli.thoucylinder.dataclasses.track.AbstractTrackUiState
import us.huseli.thoucylinder.dataclasses.track.ModalCoverTrackUiState
import us.huseli.thoucylinder.dataclasses.track.ModalCoverTrackUiStateLight
import us.huseli.thoucylinder.getAverageColor
import us.huseli.thoucylinder.repositories.Repositories
import us.huseli.thoucylinder.waveList
import javax.inject.Inject
import kotlin.time.DurationUnit

@HiltViewModel
class ModalCoverViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel() {
    val albumArtAverageColor: StateFlow<Color?> = repos.player.currentCombo.map { combo ->
        combo?.let { repos.image.getTrackComboFullImageBitmap(it) }?.getAverageColor()?.copy(alpha = 0.3f)
    }.stateWhileSubscribed()
    val isLoading: StateFlow<Boolean> = repos.player.isLoading

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentAmplitudes: StateFlow<ImmutableList<Int>> = repos.player.currentCombo.flatMapLatest { combo ->
        combo?.track?.let { repos.track.flowAmplitudes(it.trackId) } ?: emptyFlow()
    }.stateWhileSubscribed(waveList(100, 0, 12, 3).toImmutableList())

    val booleans = combine(
        repos.player.canGotoNext,
        repos.player.canPlay,
        repos.player.isLoading,
        repos.player.isPlaying,
        repos.player.isRepeatEnabled,
        repos.player.isShuffleEnabled,
    ) { booleans ->
        ModalCoverBooleans(
            canGotoNext = booleans[0],
            canPlay = booleans[1],
            isLoading = booleans[2],
            isPlaying = booleans[3],
            isRepeatEnabled = booleans[4],
            isShuffleEnabled = booleans[5],
        )
    }.distinctUntilChanged().stateWhileSubscribed(ModalCoverBooleans())

    val currentProgress: StateFlow<Float> =
        combine(repos.player.currentPositionMs, repos.player.currentCombo) { position, combo ->
            combo?.track?.duration?.toLong(DurationUnit.MILLISECONDS)
                ?.takeIf { it > 0 }
                ?.let { position / it.toFloat() }
                ?: 0f
        }.distinctUntilChanged().stateWhileSubscribed(0f)

    val nextTrackUiState: StateFlow<ModalCoverTrackUiStateLight?> = repos.player.nextCombo.map { combo ->
        combo?.let {
            ModalCoverTrackUiStateLight(
                albumArtUri = it.fullImageUrl,
                artistString = it.artistString,
                title = it.track.title,
            )
        }
    }.distinctUntilChanged().stateWhileSubscribed()

    val previousTrackUiState: StateFlow<ModalCoverTrackUiStateLight?> = repos.player.previousCombo.map { combo ->
        combo?.let {
            ModalCoverTrackUiStateLight(
                albumArtUri = it.fullImageUrl,
                artistString = it.artistString,
                title = it.track.title,
            )
        }
    }.distinctUntilChanged().stateWhileSubscribed()

    val trackUiState: StateFlow<ModalCoverTrackUiState?> = repos.player.currentCombo.map { combo ->
        if (combo != null) {
            ModalCoverTrackUiState(
                albumId = combo.track.albumId,
                albumTitle = combo.album?.title,
                artists = combo.artists
                    .map { AbstractTrackUiState.Artist.fromArtistCredit(it) }
                    .toImmutableList(),
                artistString = combo.artistString,
                durationMs = combo.track.durationMs?.takeIf { it >= 0 } ?: 0,
                fullImageUrl = combo.fullImageUrl,
                id = combo.track.trackId,
                isDownloadable = combo.track.isDownloadable,
                isInLibrary = combo.track.isInLibrary,
                isPlayable = combo.track.isPlayable,
                musicBrainzReleaseGroupId = combo.album?.musicBrainzReleaseGroupId,
                musicBrainzReleaseId = combo.album?.musicBrainzReleaseId,
                spotifyId = combo.track.spotifyId,
                spotifyWebUrl = combo.track.spotifyWebUrl,
                thumbnailUrl = combo.fullImageUrl, // we want the same file regardless of expand/collapse status
                title = combo.track.title,
                youtubeWebUrl = combo.track.youtubeWebUrl,
            )
        } else null
    }.distinctUntilChanged().stateWhileSubscribed()

    fun playOrPauseCurrent() = repos.player.playOrPauseCurrent()

    fun seekToProgress(progress: Float) = repos.player.seekToProgress(progress)

    fun skipToNext() = repos.player.skipToNext()

    fun skipToPrevious() = repos.player.skipToPrevious()

    fun skipToStartOrPrevious() = repos.player.skipToStartOrPrevious()

    fun toggleRepeat() = repos.player.toggleRepeat()

    fun toggleShuffle() = repos.player.toggleShuffle()
}
