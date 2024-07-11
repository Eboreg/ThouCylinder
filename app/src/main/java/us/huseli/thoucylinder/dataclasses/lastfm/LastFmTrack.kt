package us.huseli.thoucylinder.dataclasses.lastfm

import com.google.gson.annotations.SerializedName

data class LastFmTrack(
    val name: String,
    val url: String,
    val mbid: String? = null,
    val duration: String? = null,
    val listeners: String? = null,
    @SerializedName("playcount") val playCount: String? = null,
    val artist: LastFmArtist? = null,
    val album: LastFmTrackAlbum? = null,
    @SerializedName("userplaycount") val userPlayCount: String? = null,
    @SerializedName("userloved") val userLoved: String? = null,
    @SerializedName("toptags") val topTags: TopTags? = null,
    val wiki: LastFmWiki? = null,
) {
    data class TopTags(val tag: List<LastFmTag>)

    data class LastFmTrackAlbum(
        val mbid: String,
        val url: String,
        val title: String,
        val artist: String,
        val image: List<LastFmImage>,
    )
}
