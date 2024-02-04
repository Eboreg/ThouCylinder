package us.huseli.thoucylinder.dataclasses.entities

import android.content.Context
import android.os.Parcelable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import us.huseli.retaintheme.extensions.dpToPx
import us.huseli.retaintheme.extensions.scaleToMaxSize
import us.huseli.thoucylinder.Constants.IMAGE_MAX_DP_FULL
import us.huseli.thoucylinder.Constants.IMAGE_MAX_DP_THUMBNAIL
import us.huseli.thoucylinder.dataclasses.SpotifyAlbumArt
import us.huseli.thoucylinder.getSquareBitmapByUrl
import java.util.UUID

@Parcelize
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Album::class,
            parentColumns = ["Album_albumId"],
            childColumns = ["SpotifyAlbum_albumId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("SpotifyAlbum_albumId")],
)
data class SpotifyAlbum(
    @ColumnInfo("SpotifyAlbum_albumType") val albumType: String,
    @ColumnInfo("SpotifyAlbum_totalTracks") val totalTracks: Int,
    @ColumnInfo("SpotifyAlbum_href") val href: String,
    @ColumnInfo("SpotifyAlbum_id") @PrimaryKey val id: String,
    @ColumnInfo("SpotifyAlbum_name") val name: String,
    @ColumnInfo("SpotifyAlbum_releaseDate") val releaseDate: String,
    @ColumnInfo("SpotifyAlbum_releaseDatePrecision") val releaseDatePrecision: String,
    @ColumnInfo("SpotifyAlbum_uri") val uri: String,
    @ColumnInfo("SpotifyAlbum_albumId") val albumId: UUID? = null,
    @Embedded("SpotifyAlbum_fullImage_") val fullImage: SpotifyAlbumArt? = null,
    @Embedded("SpotifyAlbum_thumbnail_") val thumbnail: SpotifyAlbumArt? = null,
) : Parcelable {
    val year: Int?
        get() = releaseDate.substringBefore('-').toIntOrNull()

    suspend fun getFullImageBitmap(context: Context): ImageBitmap? =
        fullImage?.url?.getSquareBitmapByUrl()?.scaleToMaxSize(context.dpToPx(IMAGE_MAX_DP_FULL))?.asImageBitmap()

    suspend fun getThumbnailImageBitmap(context: Context): ImageBitmap? =
        (thumbnail?.url ?: fullImage?.url)
            ?.getSquareBitmapByUrl()
            ?.scaleToMaxSize(context.dpToPx(IMAGE_MAX_DP_THUMBNAIL))
            ?.asImageBitmap()
}
