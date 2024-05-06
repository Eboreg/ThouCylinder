package us.huseli.thoucylinder.managers

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Managers @Inject constructor(
    val image: ImageManager,
    val library: LibraryManager,
    val player: PlayerManager,
    val radio: RadioManager,
)
