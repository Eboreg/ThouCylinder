package us.huseli.thoucylinder

import androidx.navigation.NavType
import androidx.navigation.navArgument
import us.huseli.retaintheme.navigation.AbstractDestination
import us.huseli.retaintheme.navigation.AbstractSimpleDestination
import us.huseli.thoucylinder.Constants.NAV_ARG_ALBUM
import us.huseli.thoucylinder.Constants.NAV_ARG_ARTIST
import us.huseli.thoucylinder.Constants.NAV_ARG_PLAYLIST
import java.util.UUID

object SearchDestination : AbstractSimpleDestination("search")

object LibraryDestination : AbstractSimpleDestination("library")

object QueueDestination : AbstractSimpleDestination("queue")

object DebugDestination : AbstractSimpleDestination("debug")

object ImportDestination : AbstractSimpleDestination("import")

object DownloadsDestination : AbstractSimpleDestination("downloads")

object AlbumDestination : AbstractDestination() {
    override val routeTemplate = "album/{$NAV_ARG_ALBUM}"
    override val arguments = listOf(navArgument(NAV_ARG_ALBUM) { type = NavType.StringType })

    fun route(albumId: UUID) = "album/$albumId"
}

object ArtistDestination : AbstractDestination() {
    override val arguments = listOf(navArgument(NAV_ARG_ARTIST) { type = NavType.StringType })
    override val routeTemplate = "artist/{$NAV_ARG_ARTIST}"

    fun route(artist: String) = "artist/$artist"
}

object PlaylistDestination : AbstractDestination() {
    override val routeTemplate = "playlist/{$NAV_ARG_PLAYLIST}"
    override val arguments = listOf(navArgument(NAV_ARG_PLAYLIST) { type = NavType.StringType })

    fun route(playlistId: UUID) = "playlist/$playlistId"
}
