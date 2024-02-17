package us.huseli.thoucylinder.dataclasses.musicBrainz

import com.google.gson.annotations.SerializedName

data class MusicBrainzTrack(
    @SerializedName("artist-credit")
    val artistCredit: List<MusicBrainzArtistCredit>,
    override val id: String,
    val length: Int,
    val number: String,
    val position: Int,
    val recording: Recording,
    val title: String,
) : AbstractMusicBrainzItem() {
    data class Recording(
        @SerializedName("artist-credit")
        val artistCredit: List<MusicBrainzArtistCredit>,
        val disambiguation: String?,
        @SerializedName("first-release-date")
        val firstReleaseDate: String?,
        val genres: List<MusicBrainzGenre>,
        override val id: String,
        val length: Int,
        val title: String,
    ) : AbstractMusicBrainzItem()

    val artist: String?
        get() = artistCredit.artistString().takeIf { it.isNotEmpty() }

    val year: Int?
        get() = recording.firstReleaseDate
            ?.substringBefore('-')
            ?.takeIf { it.matches(Regex("^\\d{4}$")) }
            ?.toInt()
}
