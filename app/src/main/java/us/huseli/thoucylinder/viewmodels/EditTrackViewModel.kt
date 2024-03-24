package us.huseli.thoucylinder.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import us.huseli.thoucylinder.repositories.Repositories
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.views.TrackCombo
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit
import us.huseli.thoucylinder.dataclasses.views.toTrackArtists
import us.huseli.thoucylinder.launchOnIOThread
import javax.inject.Inject

@HiltViewModel
class EditTrackViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel(repos) {
    fun updateTrackCombo(
        combo: AbstractTrackCombo,
        title: String,
        year: Int?,
        albumPosition: Int?,
        discNumber: Int?,
        artistNames: Collection<String>,
    ) = launchOnIOThread {
        var trackArtists = combo.artists
        val albumCombo = combo.track.albumId?.let { repos.album.getAlbumCombo(it) }

        if (artistNames.filter { it.isNotEmpty() } != combo.artists.map { it.name }) {
            val artists = artistNames.filter { it.isNotEmpty() }.map { repos.artist.artistCache.getByName(it) }

            trackArtists = artists.map { TrackArtistCredit(artist = it, trackId = combo.track.trackId) }
            repos.artist.setTrackArtists(trackArtists.toTrackArtists())
        }

        val updatedTrack = ensureTrackMetadata(
            combo.track.copy(
                title = title,
                year = year,
                albumPosition = albumPosition,
                discNumber = discNumber,
            )
        )

        repos.track.updateTrack(updatedTrack)
        repos.localMedia.tagTrack(
            trackCombo = TrackCombo(track = updatedTrack, album = combo.album, artists = trackArtists),
            albumArtists = albumCombo?.artists,
        )
    }
}
