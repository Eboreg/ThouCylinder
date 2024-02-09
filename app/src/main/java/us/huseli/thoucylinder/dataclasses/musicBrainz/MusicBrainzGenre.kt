package us.huseli.thoucylinder.dataclasses.musicBrainz

import us.huseli.retaintheme.extensions.capitalized
import us.huseli.thoucylinder.dataclasses.entities.Genre

data class MusicBrainzGenre(
    val count: Int,
    val disambiguation: String,
    override val id: String,
    val name: String,
) : AbstractMusicBrainzItem()

fun Iterable<MusicBrainzGenre>.toInternal(): List<Genre> = map { Genre(genreName = it.name.capitalized()) }
