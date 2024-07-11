package us.huseli.thoucylinder.interfaces

import androidx.media3.common.PlaybackException
import us.huseli.thoucylinder.dataclasses.track.QueueTrackCombo
import us.huseli.thoucylinder.enums.PlaybackState
import us.huseli.thoucylinder.repositories.PlayerRepository

interface PlayerRepositoryListener {
    fun onPlayerError(
        error: PlaybackException,
        currentCombo: QueueTrackCombo?,
        lastAction: PlayerRepository.LastAction,
    ) {
    }

    fun onPlaybackChange(combo: QueueTrackCombo?, state: PlaybackState) {}

    fun onHalfTrackPlayed(combo: QueueTrackCombo, startTimestamp: Long) {}
}
