package us.huseli.thoucylinder.dataclasses.lastfm

data class LastFmTopAlbumsResponse(val topalbums: TopAlbums) {
    data class TopAlbums(val album: List<LastFmAlbum>)
}

data class LastFmTopArtistsResponse(val topartists: TopArtists) {
    data class TopArtists(val artist: List<LastFmArtist>)
}

data class LastFmTrackInfoResponse(val track: LastFmTrack)
