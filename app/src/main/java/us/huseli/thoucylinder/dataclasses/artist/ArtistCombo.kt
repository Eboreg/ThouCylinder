package us.huseli.thoucylinder.dataclasses.artist

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Embedded

@DatabaseView(
    """
    SELECT
        Artist.*,
        COUNT(DISTINCT Track_trackId) AS ArtistCombo_trackCount,
        COUNT(DISTINCT Album_albumId) AS ArtistCombo_albumCount,
        COALESCE(
            Artist_image_thumbnailUriString,
            Album_albumArt_thumbnailUriString,
            Album_youtubePlaylist_thumbnail_url,
            Album_spotifyImage_thumbnailUriString
        ) AS ArtistCombo_thumbnailUrl
    FROM Artist
        LEFT JOIN AlbumArtist ON Artist_id = AlbumArtist_artistId
        LEFT JOIN AlbumCombo ON Album_albumId = AlbumArtist_albumId AND Album_trackCount > 0 AND Album_isInLibrary = 1
            AND Album_isHidden = 0
        LEFT JOIN TrackArtist ON Artist_id = TrackArtist_artistId
        LEFT JOIN Track ON (Track_albumId = AlbumArtist_albumId OR Track_trackId = TrackArtist_trackId)
            AND Track_isInLibrary = 1
    GROUP BY Artist_id
    ORDER BY LOWER(Artist_name)
    """
)
@Immutable
data class ArtistCombo(
    @Embedded val artist: Artist,
    @ColumnInfo("ArtistCombo_albumCount") val albumCount: Int,
    @ColumnInfo("ArtistCombo_trackCount") val trackCount: Int,
    @ColumnInfo("ArtistCombo_thumbnailUrl") val thumbnailUrl: String?,
) {
    fun toUiState() = ArtistUiState(
        albumCount = albumCount,
        artistId = artist.artistId,
        name = artist.name,
        spotifyWebUrl = artist.spotifyWebUrl,
        thumbnailUri = thumbnailUrl,
        trackCount = trackCount,
    )
}
