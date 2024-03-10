package us.huseli.thoucylinder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import us.huseli.thoucylinder.repositories.AlbumRepository
import us.huseli.thoucylinder.repositories.ArtistRepository
import us.huseli.thoucylinder.repositories.DiscogsRepository
import us.huseli.thoucylinder.repositories.LastFmRepository
import us.huseli.thoucylinder.repositories.LocalMediaRepository
import us.huseli.thoucylinder.repositories.MusicBrainzRepository
import us.huseli.thoucylinder.repositories.PlayerRepository
import us.huseli.thoucylinder.repositories.PlaylistRepository
import us.huseli.thoucylinder.repositories.RadioRepository
import us.huseli.thoucylinder.repositories.SettingsRepository
import us.huseli.thoucylinder.repositories.SpotifyRepository
import us.huseli.thoucylinder.repositories.TrackDownloadRepository
import us.huseli.thoucylinder.repositories.TrackRepository
import us.huseli.thoucylinder.repositories.YoutubeRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Repositories @Inject constructor(
    val album: AlbumRepository,
    val artist: ArtistRepository,
    val discogs: DiscogsRepository,
    val download: TrackDownloadRepository,
    val lastFm: LastFmRepository,
    val localMedia: LocalMediaRepository,
    val musicBrainz: MusicBrainzRepository,
    val player: PlayerRepository,
    val playlist: PlaylistRepository,
    val radio: RadioRepository,
    val settings: SettingsRepository,
    val spotify: SpotifyRepository,
    val track: TrackRepository,
    val youtube: YoutubeRepository,
) {
    /** Stuff that needs to be shared between repositories goes here. */
    val globalScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        player.addListener(lastFm)
    }
}
