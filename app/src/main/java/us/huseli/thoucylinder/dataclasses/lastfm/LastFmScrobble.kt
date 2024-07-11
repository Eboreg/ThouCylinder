package us.huseli.thoucylinder.dataclasses.lastfm

data class LastFmScrobble(
    val artist: String,
    val track: String,
    val timestamp: Long,
    val album: String? = null,
    val trackNumber: Int? = null,
    val mbid: String? = null,
    val albumArtist: String? = null,
    val duration: Int? = null,
) {
    fun toMap(index: Int): Map<String, String> {
        val map = mutableMapOf(
            "artist[$index]" to artist,
            "track[$index]" to track,
            "timestamp[$index]" to timestamp.toString(),
        )
        if (album != null) map["album[$index]"] = album
        if (trackNumber != null) map["trackNumber[$index]"] = trackNumber.toString()
        if (mbid != null) map["mbid[$index]"] = mbid
        if (albumArtist != null && albumArtist != artist) map["albumArtist[$index]"] = albumArtist
        if (duration != null) map["duration[$index]"] = duration.toString()

        return map
    }
}
