package us.huseli.thoucylinder.dataclasses.lastfm

import com.google.gson.annotations.SerializedName
import us.huseli.thoucylinder.dataclasses.artist.UnsavedAlbumArtistCredit

data class LastFmArtist(
    val mbid: String,
    val url: String,
    @SerializedName("playcount") val playCount: String? = null,
    val name: String,
    val image: List<LastFmImage>? = null,
) {
    fun toNativeAlbumArtist(albumId: String) =
        UnsavedAlbumArtistCredit(name = name, albumId = albumId, musicBrainzId = mbid)
}
