package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import us.huseli.thoucylinder.Constants.NAV_ARG_ARTIST
import us.huseli.thoucylinder.repositories.LocalRepository
import us.huseli.thoucylinder.repositories.PlayerRepository
import us.huseli.thoucylinder.repositories.YoutubeRepository
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    repo: LocalRepository,
    savedStateHandle: SavedStateHandle,
    playerRepo: PlayerRepository,
    youtubeRepo: YoutubeRepository,
) : BaseViewModel(playerRepo, repo, youtubeRepo) {
    val artist: String = savedStateHandle.get<String>(NAV_ARG_ARTIST)!!
    val tracks = repo.tracks
        .map { tracks -> tracks.filter { it.artist == artist || (it.album?.artist == artist && it.artist == null) } }
        .distinctUntilChanged()
}
