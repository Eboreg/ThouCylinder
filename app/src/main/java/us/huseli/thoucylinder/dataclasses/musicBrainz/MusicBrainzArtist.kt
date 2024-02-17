package us.huseli.thoucylinder.dataclasses.musicBrainz

import com.google.gson.annotations.SerializedName

data class MusicBrainzArtistCredit(
    val artist: Artist,
    val joinphrase: String?,
    val name: String,
) {
    data class Artist(
        val disambiguation: String?,
        val genres: List<MusicBrainzGenre>?,
        override val id: String,
        val name: String,
        @SerializedName("sort-name")
        val sortName: String?,
        val type: String?,
        @SerializedName("type-id")
        val typeId: String?,
    ) : AbstractMusicBrainzItem()
}

fun Iterable<MusicBrainzArtistCredit>.artistString(): String = joinToString("/") { it.name }
