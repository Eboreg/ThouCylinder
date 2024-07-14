package us.huseli.thoucylinder.dataclasses.lastfm

data class LastFmTopAlbumsResponse(val topalbums: TopAlbums) {
    data class TopAlbums(val album: List<LastFmAlbum>)
}

data class LastFmTrackInfoResponse(val track: LastFmTrack)
