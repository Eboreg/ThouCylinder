package us.huseli.thoucylinder.dataclasses.spotify

abstract class AbstractSpotifyItem {
    abstract val id: String

    override fun equals(other: Any?) = other?.javaClass == javaClass && (other as AbstractSpotifyItem).id == id
    override fun hashCode() = id.hashCode()
}
