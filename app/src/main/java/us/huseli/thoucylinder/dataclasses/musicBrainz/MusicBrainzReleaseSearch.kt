package us.huseli.thoucylinder.dataclasses.musicBrainz

import com.google.gson.annotations.SerializedName

data class MusicBrainzReleaseSearch(
    val count: Int,
    val offset: Int,
    val releases: List<Release>,
) {
    data class Release(
        @SerializedName("artist-credit")
        val artistCredit: List<MusicBrainzArtistCredit>,
        val count: Int,
        val country: String,
        val date: String,
        override val id: String,
        val media: List<Media>,
        val packaging: String,
        @SerializedName("packaging-id")
        val packagingId: String,
        val quality: String?,
        @SerializedName("release-group")
        val releaseGroup: ReleaseGroup,
        val score: Int,
        val status: String,
        @SerializedName("status-id")
        val statusId: String,
        val title: String,
        @SerializedName("track-count")
        val trackCount: Int,
    ) : AbstractMusicBrainzItem() {
        data class ReleaseGroup(
            override val id: String,
            @SerializedName("primary-type")
            val primaryType: MusicBrainzReleaseGroupPrimaryType,
            @SerializedName("primary-type-id")
            val primaryTypeId: String,
            val title: String,
            @SerializedName("type-id")
            val typeId: String,
        ) : AbstractMusicBrainzItem()

        data class Media(
            @SerializedName("disc-count")
            val discCount: Int,
            val format: String,
            @SerializedName("track-count")
            val trackCount: Int,
        )
    }

    val releaseIds: List<String>
        get() = releases.map { it.id }
}
