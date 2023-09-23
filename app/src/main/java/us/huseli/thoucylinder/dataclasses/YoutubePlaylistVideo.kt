package us.huseli.thoucylinder.dataclasses

import java.io.File

data class YoutubePlaylistVideo(
    val id: String,
    val playlistId: String,
    val position: Int,
    var video: YoutubeVideo,
) {
    fun toTempTrack(localFile: File, metadata: TrackMetadata) =
        video.toTempTrack(localFile, metadata).copy(albumPosition = position)
}
