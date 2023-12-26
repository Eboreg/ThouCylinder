package us.huseli.thoucylinder.dataclasses.lastFm

data class LastFmNowPlaying(
    val artist: String,
    val track: String,
    val album: String? = null,
    val trackNumber: Int? = null,
    val mbid: String? = null,
    val albumArtist: String? = null,
    val duration: Int? = null,
) {
    fun toMap(): Map<String, String> {
        val map = mutableMapOf(
            "artist" to artist,
            "track" to track,
        )

        if (album != null) map["album"] = album
        if (trackNumber != null) map["trackNumber"] = trackNumber.toString()
        if (mbid != null) map["mbid"] = mbid
        if (albumArtist != null) map["albumArtist"] = albumArtist
        if (duration != null) map["duration"] = duration.toString()

        return map.toMap()
    }
}
