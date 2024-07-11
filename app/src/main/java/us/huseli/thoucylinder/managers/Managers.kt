package us.huseli.thoucylinder.managers

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Managers @Inject constructor(
    val external: ExternalContentManager,
    val library: LibraryManager,
    val player: PlayerManager,
    val playlist: PlaylistManager,
    val radio: RadioManager,
)
