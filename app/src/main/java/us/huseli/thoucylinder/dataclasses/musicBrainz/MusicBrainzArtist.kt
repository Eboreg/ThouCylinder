package us.huseli.thoucylinder.dataclasses.musicBrainz

import com.google.gson.annotations.SerializedName
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.views.AlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit

data class MusicBrainzArtistCredit(
    val artist: MusicBrainzArtist,
    val joinphrase: String?,
    val name: String,
) {
    data class MusicBrainzArtist(
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

    fun toNativeAlbumArtist(artist: Artist, albumId: String, position: Int) =
        AlbumArtistCredit(artist = artist, albumId = albumId)
            .copy(position = position, musicBrainzId = this.artist.id, joinPhrase = joinphrase ?: "/")

    fun toNativeTrackArtist(artist: Artist, trackId: String, position: Int) =
        TrackArtistCredit(artist = artist, trackId = trackId)
            .copy(position = position, musicBrainzId = this.artist.id, joinPhrase = joinphrase ?: "/")
}

fun List<MusicBrainzArtistCredit>.joined(): String = mapIndexed { index, artistCredit ->
    artistCredit.name + if (index < lastIndex) artistCredit.joinphrase ?: "/" else ""
}.joinToString("")
