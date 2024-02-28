package us.huseli.thoucylinder.dataclasses.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.views.AlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit
import java.util.UUID

@Parcelize
@Entity
data class Artist(
    @ColumnInfo("Artist_name") val name: String,
    @ColumnInfo("Artist_id") @PrimaryKey val id: UUID = UUID.randomUUID(),
    @ColumnInfo("Artist_spotifyId") val spotifyId: String? = null,
    @ColumnInfo("Artist_musicBrainzId") val musicBrainzId: String? = null,
    @Embedded("Artist_image_") val image: MediaStoreImage? = null,
) : Parcelable


fun Collection<Artist>.joined(): String? = takeIf { it.isNotEmpty() }?.joinToString("/") { it.name }

fun Iterable<Artist>.toAlbumArtists(albumId: UUID) =
    mapIndexed { index, artist -> AlbumArtist(albumId = albumId, artistId = artist.id, position = index) }

fun Iterable<Artist>.toAlbumArtistCredits(albumId: UUID) =
    mapIndexed { index, artist -> AlbumArtistCredit(artist = artist, albumId = albumId, position = index) }

fun Iterable<Artist>.toTrackArtistCredits(trackId: UUID) =
    mapIndexed { index, artist -> TrackArtistCredit(artist = artist, trackId = trackId, position = index) }
