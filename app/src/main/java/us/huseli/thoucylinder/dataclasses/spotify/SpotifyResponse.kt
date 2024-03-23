package us.huseli.thoucylinder.dataclasses.spotify

import com.google.gson.annotations.SerializedName

data class SpotifyResponse<T>(
    val href: String,
    val limit: Int,
    val next: String?,
    val offset: Int,
    val previous: String?,
    val total: Int,
    val items: List<T>,
)

data class SpotifySearchResponse(
    val albums: SpotifyResponse<SpotifySimplifiedAlbum>?,
    val artists: SpotifyResponse<SpotifyArtist>?,
    val tracks: SpotifyResponse<SpotifyTrack>?,
)

data class SpotifyTrackRecommendationResponse(
    val seeds: List<Seed>,
    val tracks: List<SpotifyTrack>,
) {
    data class Seed(
        val afterFilteringSize: Int,
        val afterRelinkingSize: Int,
        val href: String,
        val id: String,
        val initialPoolSize: Int,
        val type: String,
    )
}

data class SpotifyAlbumsResponse(val albums: List<SpotifyAlbum>)

data class SpotifyArtistsResponse(val artists: List<SpotifyArtist>)

data class SpotifyTrackAudioFeaturesResponse(
    @SerializedName("audio_features")
    val audioFeatures: List<SpotifyTrackAudioFeatures?>,
)
