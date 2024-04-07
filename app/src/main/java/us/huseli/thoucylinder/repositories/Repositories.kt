package us.huseli.thoucylinder.repositories

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
