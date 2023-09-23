package us.huseli.thoucylinder.dataclasses

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import us.huseli.thoucylinder.sanitizeFilename
import java.io.File
import java.util.UUID

@Parcelize
data class TempTrack(
    val title: String,
    val localFile: File,
    val metadata: TrackMetadata,
    val artist: String? = null,
    val albumPosition: Int? = null,
    val youtubeVideo: YoutubeVideo? = null,
    val image: Image? = null,
) : Parcelable {
    val basename: String
        get() {
            var name = ""
            if (albumPosition != null) name += "${String.format("%02d", albumPosition)} - "
            if (artist != null) name += "$artist - "
            name += title

            return name.sanitizeFilename()
        }

    fun toTrack(
        metadata: TrackMetadata,
        localSubdir: String? = null,
        albumId: UUID? = null,
    ): Track = Track(
        title = title,
        artist = artist,
        albumPosition = albumPosition,
        localSubdir = localSubdir,
        albumId = albumId,
        metadata = metadata,
        youtubeVideo = youtubeVideo,
        image = image,
    )
}
