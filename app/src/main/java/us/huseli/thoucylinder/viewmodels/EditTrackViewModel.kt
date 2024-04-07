package us.huseli.thoucylinder.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit
import us.huseli.thoucylinder.dataclasses.views.toTrackArtists
import us.huseli.thoucylinder.launchOnIOThread
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class EditTrackViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel(repos) {
    fun updateTrackCombo(
        state: Track.ViewState,
        title: String,
        year: Int?,
        albumPosition: Int?,
        discNumber: Int?,
        artistNames: Collection<String>,
    ) = launchOnIOThread {
        var trackArtists = state.trackArtists.toList()
        val albumCombo = state.track.albumId?.let { repos.album.getAlbumCombo(it) }

        if (artistNames.filter { it.isNotEmpty() } != state.trackArtists.map { it.name }) {
            val artists = artistNames.filter { it.isNotEmpty() }.map { repos.artist.artistCache.getByName(it) }

            trackArtists = artists.map { TrackArtistCredit(artist = it, trackId = state.track.trackId) }
            repos.artist.setTrackArtists(trackArtists.toTrackArtists())
        }

        val updatedTrack = ensureTrackMetadata(
            state.track.copy(
                title = title,
                year = year,
                albumPosition = albumPosition,
                discNumber = discNumber,
            )
        )

        repos.track.updateTrack(updatedTrack)
        repos.localMedia.tagTrack(
            track = updatedTrack,
            trackArtists = trackArtists,
            albumArtists = albumCombo?.artists,
            album = albumCombo?.album,
        )
    }
}
