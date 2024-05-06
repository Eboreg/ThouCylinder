package us.huseli.thoucylinder.dataclasses

import androidx.documentfile.provider.DocumentFile
import us.huseli.retaintheme.extensions.padStart
import us.huseli.thoucylinder.getRelativePath
import us.huseli.thoucylinder.stripArtist

data class ArtistTitlePair(
    val artist: String?,
    val title: String?,
) {
    companion object {
        fun fromDirectory(directory: DocumentFile): ArtistTitlePair {
            val pathSegments = directory.uri.getRelativePath()?.split('/')?.drop(1)
            val (pathArtist, pathTitle) = when (pathSegments?.size) {
                1 -> pathSegments[0].split(Regex(" +- +"), 2).padStart(2).toList()
                0 -> listOf(null, null)
                null -> listOf(null, null)
                else -> pathSegments.takeLast(2)
            }

            return ArtistTitlePair(artist = pathArtist, title = pathTitle?.stripArtist(pathArtist))
        }
    }
}
