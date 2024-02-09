package us.huseli.thoucylinder.dataclasses.musicBrainz

import com.google.gson.annotations.SerializedName

data class MusicBrainzReleaseGroupSearch(
    val count: Int,
    val offset: Int,
    @SerializedName("release-groups")
    val releaseGroups: List<ReleaseGroup>,
) {
    data class ReleaseGroup(
        @SerializedName("artist-credit")
        val artistCredit: List<MusicBrainzArtistCredit>,
        val count: Int,
        @SerializedName("first-release-date")
        val firstReleaseDate: String?,
        override val id: String,
        @SerializedName("primary-type")
        val primaryType: MusicBrainzReleaseGroupPrimaryType,
        @SerializedName("primary-type-id")
        val primaryTypeId: String,
        val releases: List<Release>,
        val score: Int,
        val title: String,
        @SerializedName("type-id")
        val typeId: String,
    ) : AbstractMusicBrainzItem() {
        data class Release(
            override val id: String,
            val status: MusicBrainzReleaseStatus,
            @SerializedName("status-id")
            val statusId: String,
            val title: String,
        ) : AbstractMusicBrainzItem()
    }
}
