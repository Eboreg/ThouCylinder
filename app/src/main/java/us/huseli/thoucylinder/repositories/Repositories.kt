package us.huseli.thoucylinder.repositories

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Repositories @Inject constructor(
    val album: AlbumRepository,
    val artist: ArtistRepository,
    val discogs: DiscogsRepository,
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
)
