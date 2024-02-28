package us.huseli.thoucylinder.dataclasses.spotify

data class SpotifyResponse<T>(
    val href: String,
    val limit: Int,
    val next: String?,
    val offset: Int,
    val previous: String?,
    val total: Int,
    val items: List<T>,
)

data class SpotifySearchResponse(val albums: SpotifyResponse<SpotifySimplifiedAlbum>)

data class SpotifyTrackRecommendationResponse(val tracks: List<SpotifyTrack>)

data class SpotifyAlbumsResponse(val albums: List<SpotifyAlbum>)

data class SpotifyArtistsResponse(val artists: List<SpotifyArtist>)
