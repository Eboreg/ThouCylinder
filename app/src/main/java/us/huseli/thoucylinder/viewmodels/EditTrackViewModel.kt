package us.huseli.thoucylinder.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.thoucylinder.dataclasses.uistates.TrackUiState
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class EditTrackViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractBaseViewModel() {
    fun getArtistNameSuggestions(name: String, limit: Int = 10) =
        repos.artist.getArtistNameSuggestions(name, limit)

    fun updateTrack(
        state: TrackUiState,
        title: String,
        year: Int?,
        albumPosition: Int?,
        discNumber: Int?,
        artistNames: Collection<String>,
    ) {
        launchOnIOThread {
            managers.library.updateTrack(
                uiState = state,
                title = title,
                year = year,
                albumPosition = albumPosition,
                discNumber = discNumber,
                artistNames = artistNames,
            )
        }
    }
}
