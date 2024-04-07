package us.huseli.thoucylinder.dataclasses.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.UnsavedArtist
import us.huseli.thoucylinder.dataclasses.abstr.AbstractArtist
import us.huseli.thoucylinder.dataclasses.views.AlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit
import java.util.UUID

@Parcelize
@Entity
data class Artist(
    @ColumnInfo("Artist_name") override val name: String,
    @ColumnInfo("Artist_id") @PrimaryKey override val artistId: String = UUID.randomUUID().toString(),
    @ColumnInfo("Artist_spotifyId") override val spotifyId: String? = null,
    @ColumnInfo("Artist_musicBrainzId") override val musicBrainzId: String? = null,
    @Embedded("Artist_image_") override val image: MediaStoreImage? = null,
) : Parcelable, AbstractArtist() {
    companion object {
        fun fromBase(unsavedArtist: UnsavedArtist) = Artist(
            name = unsavedArtist.name,
            spotifyId = unsavedArtist.spotifyId,
            musicBrainzId = unsavedArtist.musicBrainzId,
            image = unsavedArtist.image,
        )
    }
}

fun Collection<Artist>.joined(): String? = takeIf { it.isNotEmpty() }?.joinToString("/") { it.name }

fun Iterable<Artist>.toAlbumArtists(albumId: String) =
    mapIndexed { index, artist -> AlbumArtist(albumId = albumId, artistId = artist.artistId, position = index) }

fun Iterable<Artist>.toAlbumArtistCredits(albumId: String) =
    mapIndexed { index, artist -> AlbumArtistCredit(artist = artist, albumId = albumId, position = index) }

fun Iterable<Artist>.toTrackArtistCredits(trackId: String) =
    mapIndexed { index, artist -> TrackArtistCredit(artist = artist, trackId = trackId, position = index) }
