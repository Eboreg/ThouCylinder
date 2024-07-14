package us.huseli.thoucylinder.dataclasses.artist

interface IArtistCredit : IArtist, Comparable<IArtistCredit> {
    val joinPhrase: String
    val position: Int

    override fun compareTo(other: IArtistCredit): Int = position - other.position
}

fun Collection<IArtistCredit>.joined(): String? = takeIf { it.isNotEmpty() }
    ?.sorted()
    ?.mapIndexed { index, artist -> artist.name + if (index < size - 1) artist.joinPhrase else "" }
    ?.joinToString("")

interface ISavedArtistCredit : IArtistCredit, ISavedArtist
