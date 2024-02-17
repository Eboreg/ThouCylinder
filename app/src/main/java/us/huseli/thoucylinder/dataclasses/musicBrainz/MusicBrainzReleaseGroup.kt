package us.huseli.thoucylinder.dataclasses.musicBrainz

import com.google.gson.annotations.SerializedName

@Suppress("unused")
enum class MusicBrainzReleaseGroupPrimaryType {
    @SerializedName("Album") ALBUM,
    @SerializedName("Single") SINGLE,
    @SerializedName("EP") EP,
    @SerializedName("Broadcast") BROADCAST,
    @SerializedName("Other") OTHER,
}

data class MusicBrainzReleaseGroup(
    @SerializedName("artist-credit")
    val artistCredit: List<MusicBrainzArtistCredit>,
    val disambiguation: String?,
    @SerializedName("first-release-date")
    val firstReleaseDate: String?,
    val genres: List<MusicBrainzGenre>,
    override val id: String,
    @SerializedName("primary-type")
    val primaryType: MusicBrainzReleaseGroupPrimaryType?,
    @SerializedName("primary-type-id")
    val primaryTypeId: String?,
    val releases: List<Release>,
    val title: String,
) : AbstractMusicBrainzItem() {
    data class Release(
        @SerializedName("artist-credit")
        val artistCredit: List<MusicBrainzArtistCredit>,
        val country: String?,
        val date: String?,
        val disambiguation: String?,
        val genres: List<MusicBrainzGenre>,
        override val id: String,
        val packaging: String?,
        @SerializedName("packaging-id")
        val packagingId: String?,
        val quality: String?,
        val status: MusicBrainzReleaseStatus?,
        @SerializedName("status-id")
        val statusId: String?,
        val title: String,
    ) : AbstractMusicBrainzItem()

    val year: Int?
        get() = firstReleaseDate
            ?.substringBefore('-')
            ?.takeIf { it.matches(Regex("^\\d{4}$")) }
            ?.toInt()
}
