package us.huseli.thoucylinder.dataclasses.spotify

import com.google.gson.annotations.SerializedName

data class SpotifyUserProfile(
    @SerializedName("display_name") val displayName: String?,
    val followers: Followers,
    val href: String,
    override val id: String,
    val images: List<SpotifyImage>,
    val uri: String,
) : AbstractSpotifyItem() {
    data class Followers(val total: Int)
}
