package us.huseli.thoucylinder.dataclasses

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import us.huseli.retaintheme.sanitizeFilename
import us.huseli.thoucylinder.getMediaStoreFileNullable
import java.io.File
import java.util.UUID

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Album::class,
            parentColumns = ["albumId"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("albumId"), Index("title"), Index("artist")],
)
data class Track(
    @PrimaryKey val trackId: UUID = UUID.randomUUID(),
    val title: String,
    val isInLibrary: Boolean,
    val artist: String? = null,
    val albumId: UUID? = null,
    val albumPosition: Int? = null,
    val year: Int? = null,
    @Embedded("metadata") val metadata: TrackMetadata? = null,
    @Embedded("youtubeVideo") val youtubeVideo: YoutubeVideo? = null,
    @Embedded("image") val image: Image? = null,
    @Embedded("mediaStoreData") val mediaStoreData: MediaStoreData? = null,
    @Ignore val tempTrackData: TempTrackData? = null,
) {
    constructor(
        trackId: UUID,
        title: String,
        isInLibrary: Boolean,
        artist: String?,
        albumId: UUID?,
        albumPosition: Int?,
        year: Int?,
        metadata: TrackMetadata?,
        youtubeVideo: YoutubeVideo?,
        image: Image?,
        mediaStoreData: MediaStoreData?,
    ) : this(
        trackId = trackId,
        title = title,
        isInLibrary = isInLibrary,
        artist = artist,
        albumId = albumId,
        albumPosition = albumPosition,
        year = year,
        metadata = metadata,
        youtubeVideo = youtubeVideo,
        image = image,
        mediaStoreData = mediaStoreData,
        tempTrackData = null,
    )

    val isOnYoutube: Boolean
        get() = youtubeVideo != null

    val playUri: Uri?
        get() = mediaStoreData?.uri ?: youtubeVideo?.metadata?.uri

    fun generateBasename(): String {
        var name = ""
        if (albumPosition != null) name += "${String.format("%02d", albumPosition)} - "
        if (artist != null) name += "$artist - "
        name += title

        return name.sanitizeFilename()
    }

    val isDownloaded: Boolean
        get() = mediaStoreData != null || tempTrackData != null

    fun getContentValues() = ContentValues().apply {
        put(MediaStore.Audio.Media.TITLE, title)
        albumPosition?.also { put(MediaStore.Audio.Media.TRACK, it.toString()) }
        artist?.also { put(MediaStore.Audio.Media.ARTIST, it) }
    }

    fun toString(showAlbumPosition: Boolean, showArtist: Boolean): String {
        var string = ""
        if (albumPosition != null && showAlbumPosition) string += "$albumPosition. "
        if (artist != null && showArtist) string += "$artist - "
        string += title

        return string
    }

    override fun toString(): String = toString(showAlbumPosition = true, showArtist = true)
}


data class TempTrackData(
    val localFile: File,
)


data class MediaStoreData(
    val uri: Uri,
) {
    fun getFile(context: Context): File? = context.getMediaStoreFileNullable(uri)
}
