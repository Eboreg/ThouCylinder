package us.huseli.thoucylinder.dataclasses.lastFm

data class LastFmTopArtistsResponse(val topartists: TopArtists) {
    data class TopArtists(val artist: List<Artist>)

    data class Artist(
        val mbid: String,
        val url: String,
        val playcount: String,
        val name: String,
        val image: List<LastFmImage>,
    )
}
