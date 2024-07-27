package us.huseli.thoucylinder.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName

@Suppress("IncorrectFormatting", "unused")
enum class MusicBrainzLabelType {
    @SerializedName("Imprint") IMPRINT,
    @SerializedName("Original Production") ORIGINAL_PRODUCTION,
    @SerializedName("Bootleg Production") BOOTLEG_PRODUCTION,
    @SerializedName("Reissue Production") REISSUE_PRODUCTION,
    @SerializedName("Distributor") DISTRIBUTOR,
    @SerializedName("Holding") HOLDING,
    @SerializedName("Rights Society") RIGHTS_SOCIETY,
}

data class MusicBrainzLabelInfo(
    val label: Label,
    @SerializedName("catalog-number")
    val catalogNumber: String,
) {
    data class Label(
        override val id: String,
        val type: MusicBrainzLabelType,
        val name: String,
    ) : AbstractMusicBrainzItem()
}
