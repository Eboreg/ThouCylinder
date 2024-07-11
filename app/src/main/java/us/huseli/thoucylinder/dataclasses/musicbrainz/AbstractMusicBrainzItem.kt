package us.huseli.thoucylinder.dataclasses.musicbrainz

abstract class AbstractMusicBrainzItem {
    abstract val id: String?

    override fun equals(other: Any?) = other?.javaClass == javaClass && (other as AbstractMusicBrainzItem).id == id
    override fun hashCode() = id.hashCode()
}
