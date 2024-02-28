package us.huseli.thoucylinder

import androidx.navigation.NavType
import androidx.navigation.navArgument
import us.huseli.retaintheme.navigation.AbstractDestination
import us.huseli.retaintheme.navigation.AbstractSimpleDestination
import us.huseli.thoucylinder.Constants.NAV_ARG_ALBUM
import us.huseli.thoucylinder.Constants.NAV_ARG_ARTIST
import us.huseli.thoucylinder.Constants.NAV_ARG_PLAYLIST
import us.huseli.thoucylinder.compose.MenuItemId
import java.util.UUID

abstract class Destination(override val menuItemId: MenuItemId) :
    AbstractSimpleDestination<MenuItemId>(menuItemId.route, menuItemId)

object AddDestination : Destination(MenuItemId.SEARCH_YOUTUBE)

object LibraryDestination : Destination(MenuItemId.LIBRARY)

object QueueDestination : Destination(MenuItemId.QUEUE)

object DebugDestination : Destination(MenuItemId.DEBUG)

object ImportDestination : Destination(MenuItemId.IMPORT)

object DownloadsDestination : Destination(MenuItemId.DOWNLOADS)

object SettingsDestination : Destination(MenuItemId.SETTINGS)

object RecommendationsDestination : Destination(MenuItemId.RECOMMENDATIONS)

object AlbumDestination : AbstractDestination<MenuItemId>() {
    override val routeTemplate = "album/{$NAV_ARG_ALBUM}"
    override val arguments = listOf(navArgument(NAV_ARG_ALBUM) { type = NavType.StringType })
    override val menuItemId: MenuItemId? = null

    fun route(albumId: UUID) = "album/$albumId"
}

object ArtistDestination : AbstractDestination<MenuItemId>() {
    override val arguments = listOf(navArgument(NAV_ARG_ARTIST) { type = NavType.StringType })
    override val routeTemplate = "artist/{$NAV_ARG_ARTIST}"
    override val menuItemId: MenuItemId? = null

    fun route(artistId: UUID) = "artist/$artistId"
}

object PlaylistDestination : AbstractDestination<MenuItemId>() {
    override val routeTemplate = "playlist/{$NAV_ARG_PLAYLIST}"
    override val arguments = listOf(navArgument(NAV_ARG_PLAYLIST) { type = NavType.StringType })
    override val menuItemId: MenuItemId? = null

    fun route(playlistId: UUID) = "playlist/$playlistId"
}
