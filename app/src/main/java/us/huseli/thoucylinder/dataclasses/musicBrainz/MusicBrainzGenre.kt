package us.huseli.thoucylinder.dataclasses.musicBrainz

import us.huseli.retaintheme.extensions.capitalized
import us.huseli.thoucylinder.dataclasses.entities.Tag

data class MusicBrainzGenre(
    val count: Int,
    val disambiguation: String?,
    override val id: String,
    val name: String,
) : AbstractMusicBrainzItem()

fun Iterable<MusicBrainzGenre>.toInternal(): List<Tag> =
    map { Tag(name = it.name.capitalized(), isMusicBrainzGenre = true) }
