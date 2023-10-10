package us.huseli.thoucylinder.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.dataclasses.QueueTrackPojo
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(private val repos: Repositories) : BaseViewModel(repos) {
    val queue = repos.player.queue
    val playerCurrentPojo = repos.player.currentPojo
    val playerPlaybackState = repos.player.playbackState
    val playerCurrentPositionMs = repos.player.currentPositionMs
    val playerCanGotoNext = repos.player.canGotoNext
    val playerCanPlay = repos.player.canPlay

    fun play(pojo: QueueTrackPojo) = repos.player.play(pojo)

    fun playOrPauseCurrent() = repos.player.playOrPauseCurrent()

    fun removeFromQueue(selection: Selection) = repos.player.removeFromQueue(selection.queueTracks)

    fun skipToNext() = repos.player.skipToNext()
}
