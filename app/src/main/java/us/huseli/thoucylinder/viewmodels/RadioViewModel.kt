package us.huseli.thoucylinder.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import us.huseli.thoucylinder.dataclasses.radio.RadioDialogUiState
import us.huseli.thoucylinder.dataclasses.radio.RadioUiState
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
    private val _activeAlbumCombo = _activeAlbumId.map { albumId -> albumId?.let { repos.album.getAlbumCombo(it) } }
    private val _activeArtist = _activeArtistId.map { artistId -> artistId?.let { repos.artist.getArtist(artistId) } }

    val radioDialogUiState = combine(
        _activeAlbumCombo,
        _activeArtist,
        managers.radio.activeRadioCombo,
        repos.player.currentCombo,
        repos.settings.libraryRadioNovelty,
    ) { albumCombo, artist, radio, trackCombo, novelty ->
        RadioDialogUiState(
            activeRadio = radio?.let { RadioUiState(type = it.type, title = it.title) },
            activeAlbum = albumCombo?.takeIf { it.album.albumId != radio?.album?.albumId }?.let { combo ->
                RadioDialogUiState.Album(
                    albumId = combo.album.albumId,
                    title = combo.album.title,
                    artists = combo.artists
                        .filter { it.name != radio?.artist?.name }
                        .toImmutableList(),
                )
            },
            activeArtist = artist?.takeIf { it.artistId != radio?.artist?.artistId },
            activeTrack = trackCombo?.takeIf { it.track.trackId != radio?.track?.trackId }?.let { combo ->
                RadioDialogUiState.Track(
                    trackId = combo.track.trackId,
                    title = combo.track.title,
                    artists = combo.trackArtists
                        .filter { it.name != radio?.artist?.name }
                        .toImmutableList(),
                    album = combo.album
                        ?.takeIf { it.albumId != radio?.album?.albumId }
                        ?.let { RadioDialogUiState.Album(albumId = it.albumId, title = it.title) },
                )
            },
            libraryRadioNovelty = novelty,
        )
    }.stateLazily(RadioDialogUiState(libraryRadioNovelty = repos.settings.libraryRadioNovelty.value))

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
