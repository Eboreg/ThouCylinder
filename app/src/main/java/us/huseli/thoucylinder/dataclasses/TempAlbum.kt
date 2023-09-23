package us.huseli.thoucylinder.dataclasses

import us.huseli.thoucylinder.sanitizeFilename

data class TempAlbum(
    val title: String,
    val artist: String? = null,
    val youtubePlaylist: YoutubePlaylist? = null,
    val albumArt: Image? = null,
    val tracks: List<TempTrack> = emptyList(),
) {
    val subdirName = (artist?.let { "$artist - $title" } ?: title).sanitizeFilename()
}
