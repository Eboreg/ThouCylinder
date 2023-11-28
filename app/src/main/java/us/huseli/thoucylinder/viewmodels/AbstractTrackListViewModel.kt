package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackPojo
import us.huseli.thoucylinder.repositories.Repositories

abstract class AbstractTrackListViewModel(
    selectionKey: String,
    private val repos: Repositories,
) : AbstractSelectViewModel(selectionKey, repos) {
    open val trackDownloadTasks = repos.trackDownloadPool.tasks

    fun enqueueTrackPojo(pojo: AbstractTrackPojo, context: Context) = enqueueTrackPojos(listOf(pojo), context)

    fun enqueueTrackPojos(pojos: List<AbstractTrackPojo>, context: Context) = viewModelScope.launch {
        repos.player.insertNext(getQueueTrackPojos(pojos, repos.player.nextItemIndex))
        SnackbarEngine.addInfo(
            context.resources.getQuantityString(R.plurals.x_tracks_enqueued_next, pojos.size, pojos.size)
        )
    }

    fun playTrackPojo(pojo: AbstractTrackPojo) = viewModelScope.launch {
        getQueueTrackPojo(pojo, repos.player.nextItemIndex)?.also { repos.player.insertNextAndPlay(it) }
    }

    fun playTrackPojos(pojos: List<AbstractTrackPojo>, startIndex: Int = 0) = viewModelScope.launch {
        repos.player.replaceAndPlay(getQueueTrackPojos(pojos), startIndex)
    }
}
