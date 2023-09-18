package us.huseli.thoucylinder.data.entities

import us.huseli.thoucylinder.sanitizeFilename

data class YoutubePlaylist(
    val title: String,
    val id: String,
    val artist: String? = null,
    val videos: List<YoutubePlaylistVideo> = emptyList(),
    val thumbnail: YoutubeThumbnail? = null,
    val videoCount: Int = videos.size,
) {
    fun generateDirName(): String =
        "${artist?.let { "$it - " } ?: ""}$title".sanitizeFilename()

    fun toAlbum(): Album = Album(
        title = title,
        artist = artist,
        localPath = generateDirName(),
        youtubePlaylist = this,
        youtubeThumbnail = thumbnail,
        tracks = videos.map { it.video.toTrack(artist = artist) },
    )

    override fun toString(): String {
        return "${artist?.let { "$it - $title" } ?: title} ($videoCount videos)"
    }
}
