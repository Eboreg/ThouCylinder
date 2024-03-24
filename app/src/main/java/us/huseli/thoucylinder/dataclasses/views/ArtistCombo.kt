package us.huseli.thoucylinder.dataclasses.views

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.DatabaseView
import androidx.room.Embedded
import us.huseli.thoucylinder.dataclasses.entities.Artist

@DatabaseView(
    """
    SELECT
        Artist.*,
        COUNT(DISTINCT Track_trackId) AS trackCount,
        COUNT(DISTINCT Album_albumId) AS albumCount,
        group_concat(DISTINCT quote(Album_albumArt_uri)) AS albumArtUris,
        group_concat(DISTINCT quote(Album_youtubePlaylist_fullImage_url)) AS youtubeFullImageUrls,
        group_concat(DISTINCT quote(Album_spotifyImage_uri)) AS spotifyFullImageUrls
    FROM Artist
        LEFT JOIN AlbumArtist ON Artist_id = AlbumArtist_artistId
        LEFT JOIN Album ON Album_albumId = AlbumArtist_albumId AND Album_isInLibrary = 1
        LEFT JOIN TrackArtist ON Artist_id = TrackArtist_artistId
        LEFT JOIN Track ON (Track_albumId = AlbumArtist_albumId OR Track_trackId = TrackArtist_trackId) AND Track_isInLibrary = 1
    GROUP BY Artist_id
    HAVING trackCount > 0 OR albumCount > 0
    ORDER BY LOWER(Artist_name)
    """
)
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
