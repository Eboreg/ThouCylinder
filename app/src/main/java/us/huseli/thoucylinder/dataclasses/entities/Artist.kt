package us.huseli.thoucylinder.dataclasses.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.BaseArtist
import us.huseli.thoucylinder.dataclasses.views.AlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit
import java.util.UUID

@Parcelize
@Entity
data class Artist(
    @ColumnInfo("Artist_name") override val name: String,
    @ColumnInfo("Artist_id") @PrimaryKey val id: UUID = UUID.randomUUID(),
    @ColumnInfo("Artist_spotifyId") override val spotifyId: String? = null,
    @ColumnInfo("Artist_musicBrainzId") override val musicBrainzId: String? = null,
    @ColumnInfo("Artist_isVarious") val isVariousArtists: Boolean =
        name.lowercase().startsWith("various") || name.lowercase() == "va" || name.lowercase() == "v/a",
    @Embedded("Artist_image_") override val image: MediaStoreImage? = null,
) : Parcelable, BaseArtist(name, spotifyId, musicBrainzId, image) {
    override fun equals(other: Any?) =
        other is Artist && other.name == name && other.id == id && other.isVariousArtists == isVariousArtists

    override fun hashCode(): Int = 31 * (31 * name.hashCode() + id.hashCode()) + isVariousArtists.hashCode()

    companion object {
        fun fromBase(baseArtist: BaseArtist) = if (baseArtist is Artist) baseArtist else Artist(
            name = baseArtist.name,
            spotifyId = baseArtist.spotifyId,
            musicBrainzId = baseArtist.musicBrainzId,
            image = baseArtist.image,
        )
    }
}


fun Collection<Artist>.joined(): String? = takeIf { it.isNotEmpty() }?.joinToString("/") { it.name }

fun Iterable<Artist>.toAlbumArtists(albumId: UUID) =
    mapIndexed { index, artist -> AlbumArtist(albumId = albumId, artistId = artist.id, position = index) }

fun Iterable<Artist>.toAlbumArtistCredits(albumId: UUID) =
    mapIndexed { index, artist -> AlbumArtistCredit(artist = artist, albumId = albumId, position = index) }

fun Iterable<Artist>.toTrackArtistCredits(trackId: UUID) =
    mapIndexed { index, artist -> TrackArtistCredit(artist = artist, trackId = trackId, position = index) }
