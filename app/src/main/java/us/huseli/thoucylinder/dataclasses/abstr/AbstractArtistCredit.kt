package us.huseli.thoucylinder.dataclasses.abstr

import us.huseli.thoucylinder.dataclasses.entities.Artist

abstract class AbstractArtistCredit : Comparable<AbstractArtistCredit>, AbstractArtist() {
    abstract val joinPhrase: String
    abstract val position: Int

    override fun compareTo(other: AbstractArtistCredit): Int = position - other.position

    fun toArtist() =
        Artist(name = name, artistId = artistId, spotifyId = spotifyId, musicBrainzId = musicBrainzId, image = image)
}

fun Collection<AbstractArtistCredit>.joined(): String? = takeIf { it.isNotEmpty() }
    ?.sorted()
    ?.mapIndexed { index, artist -> artist.name + if (index < size - 1) artist.joinPhrase else "" }
    ?.joinToString("")

fun Iterable<AbstractArtistCredit>.toArtists(): Collection<Artist> = map { it.toArtist() }.toSet()
