package us.huseli.thoucylinder

import androidx.navigation.NavType
import androidx.navigation.navArgument
import us.huseli.thoucylinder.Constants.NAV_ARG_ALBUM
import us.huseli.thoucylinder.Constants.NAV_ARG_PLAYLIST
import us.huseli.thoucylinder.dataclasses.Album
import us.huseli.thoucylinder.dataclasses.YoutubePlaylist

open class Destination(val route: String)

object SearchDestination : Destination("search")

object LibraryDestination : Destination("library")

object YoutubePlaylistDestination {
    const val routeTemplate = "playlist/{$NAV_ARG_PLAYLIST}"
    val arguments = listOf(navArgument(NAV_ARG_PLAYLIST) { type = NavType.StringType })

    fun route(playlist: YoutubePlaylist) = "playlist/${playlist.id}"
}

object AlbumDestination {
    const val routeTemplate = "album/{$NAV_ARG_ALBUM}"
    val arguments = listOf(navArgument(NAV_ARG_ALBUM) { type = NavType.StringType })

    fun route(album: Album) = "album/${album.id}"
}
