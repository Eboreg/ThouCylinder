package us.huseli.thoucylinder.repositories

import androidx.media3.common.PlaybackException
import us.huseli.thoucylinder.dataclasses.pojos.QueueTrackPojo

interface PlayerRepositoryListener {
    fun onPlayerError(
        error: PlaybackException,
        currentPojo: QueueTrackPojo?,
        lastAction: PlayerRepository.LastAction,
    ): Any
}
