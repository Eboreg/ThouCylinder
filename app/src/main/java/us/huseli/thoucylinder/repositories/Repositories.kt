package us.huseli.thoucylinder.repositories

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Repositories @Inject constructor(
    val discogs: DiscogsRepository,
    val mediaStore: MediaStoreRepository,
    val player: PlayerRepository,
    val room: RoomRepository,
    val settings: SettingsRepository,
    val spotify: SpotifyRepository,
    val trackDownload: TrackDownloadRepository,
    val youtube: YoutubeRepository,
)
