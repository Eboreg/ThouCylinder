package us.huseli.thoucylinder.interfaces

import androidx.media3.common.PlaybackException
import us.huseli.thoucylinder.PlaybackState
import us.huseli.thoucylinder.dataclasses.views.QueueTrackCombo
import us.huseli.thoucylinder.repositories.PlayerRepository

interface PlayerRepositoryListener {
    fun onPlayerError(
        error: PlaybackException,
        currentCombo: QueueTrackCombo?,
        lastAction: PlayerRepository.LastAction,
    ) {
    }

    suspend fun onPlaybackChange(combo: QueueTrackCombo?, state: PlaybackState) {}

    suspend fun onHalfTrackPlayed(combo: QueueTrackCombo, startTimestamp: Long) {}
}
