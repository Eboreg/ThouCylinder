package us.huseli.thoucylinder.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import us.huseli.thoucylinder.dataclasses.uistates.RadioDialogUiState
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class RadioViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractBaseViewModel() {
    private val _activeAlbumId = MutableStateFlow<String?>(null)
    private val _activeArtistId = MutableStateFlow<String?>(null)

    val radioDialogUiState = combine(
        _activeAlbumId,
        _activeArtistId,
        repos.radio.activeRadio,
        repos.player.currentCombo,
        repos.settings.libraryRadioNovelty,
    ) { albumId, artistId, radio, trackCombo, novelty ->
        val albumCombo = albumId?.let { repos.album.getAlbumCombo(it) }
        val artist = artistId?.let { repos.artist.getArtist(it) }

        RadioDialogUiState(
            activeRadio = radio,
            activeAlbum = albumCombo?.let {
                RadioDialogUiState.Album(
                    albumId = it.album.albumId,
                    title = it.album.title,
                    artists = it.artists.toImmutableList(),
                )
            },
            activeArtist = artist,
            activeTrack = trackCombo?.let { combo ->
                RadioDialogUiState.Track(
                    trackId = combo.track.trackId,
                    title = combo.track.title,
                    artists = combo.artists.toImmutableList(),
                    album = combo.album?.let { RadioDialogUiState.Album(albumId = it.albumId, title = it.title) },
                )
            },
            libraryRadioNovelty = novelty,
        )
    }
        .distinctUntilChanged()
        .stateLazily(RadioDialogUiState(libraryRadioNovelty = repos.settings.libraryRadioNovelty.value))

    fun deactivate() = managers.radio.deactivateRadio()

    fun setActiveAlbumId(albumId: String?) {
        _activeAlbumId.value = albumId
    }

    fun setActiveArtistId(artistId: String?) {
        _activeArtistId.value = artistId
    }

    fun setLibraryRadioNovelty(value: Float) = repos.settings.setLibraryRadioNovelty(value)

    fun startAlbumRadio(albumId: String) = managers.radio.startAlbumRadio(albumId)

    fun startArtistRadio(artistId: String) = managers.radio.startArtistRadio(artistId)

    fun startLibraryRadio() = managers.radio.startLibraryRadio()

    fun startTrackRadio(trackId: String) = managers.radio.startTrackRadio(trackId)
}
