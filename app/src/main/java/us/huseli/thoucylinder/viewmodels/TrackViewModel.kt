package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.dataclasses.Track
import us.huseli.thoucylinder.repositories.LocalRepository
import us.huseli.thoucylinder.repositories.PlayerRepository
import javax.inject.Inject

@HiltViewModel
class TrackViewModel @Inject constructor(
    private val repo: LocalRepository,
    private val playerRepo: PlayerRepository,
) : ViewModel() {
    private val _track = MutableStateFlow<Track?>(null)

    val isPlaying = combine(
        playerRepo.currentUri.filterNotNull(),
        playerRepo.isPlaying,
        _track
    ) { uri, isPlaying, track ->
        isPlaying && uri == track?.localUri
    }

    fun playOrPause() = viewModelScope.launch {
        repo.getLocalTrackUri(_track.value)?.let { uri -> playerRepo.playOrPause(uri) }
    }

    fun setTrack(value: Track) {
        _track.value = value
    }
}
