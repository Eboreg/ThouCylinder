package us.huseli.thoucylinder

import androidx.media3.common.PlaybackException
import us.huseli.thoucylinder.dataclasses.combos.QueueTrackCombo
import us.huseli.thoucylinder.repositories.PlayerRepository

interface PlayerRepositoryListener {
    fun onPlayerError(
        error: PlaybackException,
        currentCombo: QueueTrackCombo?,
        lastAction: PlayerRepository.LastAction,
    ) {
    }

    suspend fun onPlaybackChange(combo: QueueTrackCombo?, state: PlayerRepository.PlaybackState) {}

    suspend fun onHalfTrackPlayed(combo: QueueTrackCombo, startTimestamp: Long) {}
}
