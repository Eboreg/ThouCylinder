package us.huseli.thoucylinder.dataclasses.abstr

import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.entities.Artist
import java.util.UUID

abstract class AbstractArtistCredit : Comparable<AbstractArtistCredit> {
    abstract val artistId: UUID
    abstract val name: String
    abstract val spotifyId: String?
    abstract val musicBrainzId: String?
    abstract val joinPhrase: String
    abstract val image: MediaStoreImage?
    abstract val position: Int

    override fun compareTo(other: AbstractArtistCredit): Int = position - other.position

    fun toArtist() =
        Artist(name = name, id = artistId, spotifyId = spotifyId, musicBrainzId = musicBrainzId, image = image)
}

fun List<AbstractArtistCredit>.joined(): String? = takeIf { it.isNotEmpty() }
    ?.sorted()
    ?.mapIndexed { index, pojo -> pojo.name + if (index < lastIndex) pojo.joinPhrase else "" }
    ?.joinToString("")

fun Iterable<AbstractArtistCredit>.toArtists(): List<Artist> = map { it.toArtist() }
