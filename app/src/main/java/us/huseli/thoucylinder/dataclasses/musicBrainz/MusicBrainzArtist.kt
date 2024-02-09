package us.huseli.thoucylinder.dataclasses.musicBrainz

import com.google.gson.annotations.SerializedName

data class MusicBrainzArtist(
    val disambiguation: String,
    val genres: List<MusicBrainzGenre>?,
    override val id: String,
    val name: String,
    @SerializedName("sort-name")
    val sortName: String,
    val type: String?,
    @SerializedName("type-id")
    val typeId: String?,
) : AbstractMusicBrainzItem()

data class MusicBrainzArtistCredit(
    val artist: MusicBrainzArtist,
    val joinphrase: String?,
    val name: String,
)

fun Iterable<MusicBrainzArtistCredit>.artistString(): String = joinToString("/") { it.name }
