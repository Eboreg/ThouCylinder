package us.huseli.thoucylinder.dataclasses.combos

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.Embedded
import us.huseli.thoucylinder.dataclasses.entities.Artist

data class ArtistCombo(
    @Embedded val artist: Artist,
    val albumCount: Int,
    val trackCount: Int,
    val albumArtUris: String,
    val youtubeFullImageUrls: String,
    val spotifyFullImageUrls: String,
) {
    fun listAlbumArtUris(): List<Uri> {
        return albumArtUris
            .trim('\'')
            .split(splitRegex)
            .filter { it != "NULL" }
            .map { it.toUri() }
    }

    fun listFullImageUrls(): List<String> {
        val urls = mutableListOf<String>()

        urls.addAll(youtubeFullImageUrls.trim('\'').split(splitRegex))
        urls.addAll(spotifyFullImageUrls.trim('\'').split(splitRegex))
        return urls.filter { it != "NULL" }
    }

    companion object {
        val splitRegex = Regex("(?<!')','(?!')")
    }
}
