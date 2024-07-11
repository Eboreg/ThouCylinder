package us.huseli.thoucylinder.dataclasses.musicbrainz

import us.huseli.retaintheme.extensions.capitalized
import us.huseli.thoucylinder.dataclasses.tag.Tag

data class MusicBrainzGenre(
    val count: Int,
    val disambiguation: String?,
    override val id: String,
    val name: String,
) : AbstractMusicBrainzItem()

fun Iterable<MusicBrainzGenre>.toInternal(): List<Tag> =
    map { Tag(name = capitalizeGenreName(it.name), isMusicBrainzGenre = true) }

fun capitalizeGenreName(name: String): String {
    val specialCases = listOf(
        "AOR",
        "ASMR",
        "EAI",
        "EBM",
        "EDM",
        "FM Synthesis",
        "Hi-NRG",
        "IDM",
        "MPB",
        "OPM",
        "RKT",
        "Trap EDM",
        "UK Drill",
        "UK Funky",
        "UK Garage",
        "UK Hardcore",
        "UK Jackin",
        "UK Street Soul",
        "UK82",
    ).associateBy { it.lowercase() }

    return specialCases[name.lowercase()] ?: name.capitalized()
}
