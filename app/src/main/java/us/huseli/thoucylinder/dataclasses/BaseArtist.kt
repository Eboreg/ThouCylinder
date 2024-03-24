package us.huseli.thoucylinder.dataclasses

open class BaseArtist(
    open val name: String,
    open val spotifyId: String? = null,
    open val musicBrainzId: String? = null,
    open val image: MediaStoreImage? = null,
) {
    val spotifyWebUrl: String?
        get() = spotifyId?.let { "https://open.spotify.com/artist/$it" }

    override fun equals(other: Any?) =
        other is BaseArtist && other.name == name

    override fun hashCode(): Int = name.hashCode()
}
