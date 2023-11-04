package us.huseli.thoucylinder.repositories

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Repositories @Inject constructor(
    val room: RoomRepository,
    val discogs: DiscogsRepository,
    val mediaStore: MediaStoreRepository,
    val player: PlayerRepository,
    val youtube: YoutubeRepository,
)
