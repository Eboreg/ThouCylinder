package us.huseli.thoucylinder.dataclasses.artist

data class ArtistUiState(
    val albumCount: Int,
    val artistId: String,
    val name: String,
    val spotifyWebUrl: String?,
    val thumbnailUri: String?,
    val trackCount: Int,
)
