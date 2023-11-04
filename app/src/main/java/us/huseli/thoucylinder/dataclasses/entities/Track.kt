package us.huseli.thoucylinder.dataclasses.entities

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import us.huseli.retaintheme.sanitizeFilename
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.TrackMetadata
import us.huseli.thoucylinder.dataclasses.YoutubeVideo
import us.huseli.thoucylinder.getMediaStoreFileNullable
import java.io.File
import java.util.UUID

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Album::class,
            parentColumns = ["Album_albumId"],
            childColumns = ["Track_albumId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("Track_albumId"), Index("Track_title"), Index("Track_artist"), Index("Track_isInLibrary")],
)
data class Track(
    @ColumnInfo("Track_trackId") @PrimaryKey val trackId: UUID = UUID.randomUUID(),
    @ColumnInfo("Track_title") val title: String,
    @ColumnInfo("Track_isInLibrary") val isInLibrary: Boolean,
    @ColumnInfo("Track_artist") val artist: String? = null,
    @ColumnInfo("Track_albumId") val albumId: UUID? = null,
    @ColumnInfo("Track_albumPosition") val albumPosition: Int? = null,
    @ColumnInfo("Track_discNumber") val discNumber: Int? = null,
    @ColumnInfo("Track_year") val year: Int? = null,
    @Embedded("Track_metadata_") val metadata: TrackMetadata? = null,
    @Embedded("Track_youtubeVideo_") val youtubeVideo: YoutubeVideo? = null,
    @Embedded("Track_image_") val image: MediaStoreImage? = null,
    @Embedded("Track_mediaStoreData_") val mediaStoreData: MediaStoreData? = null,
    @Ignore val tempTrackData: TempTrackData? = null,
) : Comparable<Track> {
    constructor(
        trackId: UUID,
        title: String,
        isInLibrary: Boolean,
        artist: String?,
        albumId: UUID?,
        albumPosition: Int?,
        discNumber: Int?,
        year: Int?,
        metadata: TrackMetadata?,
        youtubeVideo: YoutubeVideo?,
        image: MediaStoreImage?,
        mediaStoreData: MediaStoreData?,
    ) : this(
        trackId = trackId,
        title = title,
        isInLibrary = isInLibrary,
        artist = artist,
        albumId = albumId,
        albumPosition = albumPosition,
        discNumber = discNumber,
        year = year,
        metadata = metadata,
        youtubeVideo = youtubeVideo,
        image = image,
        mediaStoreData = mediaStoreData,
        tempTrackData = null,
    )

    val playUri: Uri?
        get() = mediaStoreData?.uri ?: youtubeVideo?.metadata?.uri

    val discNumberNonNull: Int
        get() = discNumber ?: 1

    private val albumPositionNonNull: Int
        get() = albumPosition ?: 0

    val isOnYoutube: Boolean
        get() = youtubeVideo != null

    val isDownloaded: Boolean
        get() = mediaStoreData != null || tempTrackData != null

    val isDownloadable: Boolean
        get() = !isDownloaded && isOnYoutube

    fun generateBasename(): String {
        var name = ""
        if (albumPosition != null) name += "${String.format("%02d", albumPosition)} - "
        if (artist != null) name += "$artist - "
        name += title

        return name.sanitizeFilename()
    }

    fun getContentValues() = ContentValues().apply {
        put(MediaStore.Audio.Media.TITLE, title)
        albumPosition?.also { put(MediaStore.Audio.Media.TRACK, it.toString()) }
        artist?.also { put(MediaStore.Audio.Media.ARTIST, it) }
    }

    suspend fun getFullImage(context: Context): Bitmap? = image?.getBitmap(context) ?: youtubeVideo?.getBitmap()

    suspend fun getThumbnail(context: Context): Bitmap? =
        image?.getThumbnailBitmap(context) ?: youtubeVideo?.getBitmap()

    fun toString(showAlbumPosition: Boolean, showArtist: Boolean): String {
        var string = ""
        if (albumPosition != null && showAlbumPosition) string += "$albumPosition. "
        if (artist != null && showArtist) string += "$artist - "
        string += title

        return string
    }

    override fun compareTo(other: Track): Int {
        if (discNumberNonNull != other.discNumberNonNull)
            return discNumberNonNull - other.discNumberNonNull
        if (albumPositionNonNull != other.albumPositionNonNull)
            return albumPositionNonNull - other.albumPositionNonNull
        return title.compareTo(other.title)
    }

    override fun toString(): String = toString(showAlbumPosition = true, showArtist = true)
}


data class TempTrackData(
    val localFile: File,
)


data class MediaStoreData(
    val uri: Uri,
    val relativePath: String,
) {
    fun getFile(context: Context): File? = context.getMediaStoreFileNullable(uri)
}
