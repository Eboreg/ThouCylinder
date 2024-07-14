package us.huseli.thoucylinder.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName

data class MusicBrainzReleaseGroupBrowse(
    @SerializedName("release-groups")
    val releaseGroups: List<ReleaseGroup>,
    @SerializedName("release-group-offset")
    val releaseGroupOffset: Int,
    @SerializedName("release-group-count")
    val releaseGroupCount: Int,
) {
    data class ReleaseGroup(
        override val id: String,
        @SerializedName("first-release-date")
        override val firstReleaseDate: String?,
        @SerializedName("primary-type")
        override val primaryType: MusicBrainzReleaseGroupPrimaryType?,
        override val title: String,
        @SerializedName("secondary-types")
        override val secondaryTypes: List<MusicBrainzReleaseGroupSecondaryType>,
    ) : AbstractMusicBrainzReleaseGroup() {
        companion object {
            val comparator: java.util.Comparator<ReleaseGroup> =
                Comparator<ReleaseGroup> { a, b -> (a?.secondaryTypes?.size ?: 0) - (b?.secondaryTypes?.size ?: 0) }
                    .thenComparator { a, b ->
                        (a.primaryType?.sortPrio ?: Int.MAX_VALUE) - (b.primaryType?.sortPrio ?: Int.MAX_VALUE)
                    }
                    .thenComparator { a, b -> (b.year ?: 0) - (a.year ?: 0) }
        }

        override val artistCredit: List<MusicBrainzArtistCredit>?
            get() = null
    }
}
