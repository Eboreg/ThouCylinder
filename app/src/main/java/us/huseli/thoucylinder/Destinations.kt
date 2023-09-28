package us.huseli.thoucylinder

import androidx.navigation.NavType
import androidx.navigation.navArgument
import us.huseli.retaintheme.navigation.AbstractDestination
import us.huseli.retaintheme.navigation.AbstractSimpleDestination
import us.huseli.thoucylinder.Constants.NAV_ARG_ALBUM
import java.util.UUID

object SearchDestination : AbstractSimpleDestination("search")

object LibraryDestination : AbstractSimpleDestination("library")

object AlbumDestination : AbstractDestination() {
    override val routeTemplate = "album/{$NAV_ARG_ALBUM}"
    override val arguments = listOf(navArgument(NAV_ARG_ALBUM) { type = NavType.StringType })

    fun route(albumId: UUID) = "album/$albumId"
}
