package us.huseli.thoucylinder.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName

data class MusicBrainzArtistSearch(
    val count: Int,
    val offset: Int,
    val artists: List<Artist>,
) {
    data class Artist(
        val id: String,
        val type: String?,
        @SerializedName("type-id")
        val typeId: String?,
        val score: Int,
        val name: String,
        val gender: String?,
        @SerializedName("gener-id")
        val genderId: String?,
        @SerializedName("sort-name")
        val sortName: String?,
        val country: String?,
        val area: Area?,
        @SerializedName("begin-area")
        val beginArea: Area?,
        val disambiguation: String?,
        @SerializedName("life-span")
        val lifeSpan: LifeSpan?,
        val aliases: List<Alias>?,
        val tags: List<Tag>?,
        val ipis: List<String>?,
        val isnis: List<String>?,
    ) {
        data class Tag(
            val count: Int,
            val name: String,
        )

        data class Alias(
            @SerializedName("sort-name")
            val sortName: String,
            val name: String,
            val locale: String?,
            val type: String?,
            @SerializedName("type-id")
            val typeId: String?,
            val primary: Boolean?,
            @SerializedName("begin-date")
            val beginDate: String?,
            @SerializedName("end-date")
            val endDate: String?,
        )

        data class Area(
            val id: String,
            val name: String,
            @SerializedName("sort-name")
            val sortName: String?,
            val type: String?,
            @SerializedName("type-id")
            val typeId: String?,
            @SerializedName("life-span")
            val lifeSpan: LifeSpan?,
        )

        data class LifeSpan(
            val begin: String?,
            val end: String?,
            val ended: Boolean?,
        )

        fun matches(name: String): Boolean {
            if (name.lowercase() == this.name.lowercase()) return true
            if (name.lowercase() == sortName?.lowercase()) return true
            return aliases?.any {
                it.name.lowercase() == name.lowercase() || it.sortName.lowercase() == name.lowercase()
            } == true
        }
    }
}
