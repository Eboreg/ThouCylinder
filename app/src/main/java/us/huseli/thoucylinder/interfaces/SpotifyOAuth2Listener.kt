package us.huseli.thoucylinder.interfaces

interface SpotifyOAuth2Listener {
    fun onSpotifyReauthNeeded(authUrl: String)
}
