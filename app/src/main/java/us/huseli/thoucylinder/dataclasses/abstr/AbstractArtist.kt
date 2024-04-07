package us.huseli.thoucylinder.dataclasses.abstr

import us.huseli.thoucylinder.dataclasses.MediaStoreImage

abstract class AbstractArtist {
    abstract val name: String
    abstract val artistId: String
    abstract val spotifyId: String?
    abstract val musicBrainzId: String?
    abstract val image: MediaStoreImage?

    val spotifyWebUrl: String?
        get() = spotifyId?.let { "https://open.spotify.com/artist/$it" }

    override fun equals(other: Any?) =
        other is AbstractArtist && other.name == name && other.artistId == artistId

    override fun hashCode(): Int = 31 * name.hashCode() + artistId.hashCode()
}
