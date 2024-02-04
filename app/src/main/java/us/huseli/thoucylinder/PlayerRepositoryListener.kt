package us.huseli.thoucylinder

import androidx.media3.common.PlaybackException
import us.huseli.thoucylinder.dataclasses.pojos.QueueTrackPojo
import us.huseli.thoucylinder.repositories.PlayerRepository

interface PlayerRepositoryListener {
    fun onPlayerError(
        error: PlaybackException,
        currentPojo: QueueTrackPojo?,
        lastAction: PlayerRepository.LastAction,
    ) {
    }

    suspend fun onPlaybackChange(pojo: QueueTrackPojo?, state: PlayerRepository.PlaybackState) {}

    suspend fun onHalfTrackPlayed(pojo: QueueTrackPojo, startTimestamp: Long) {}
}
