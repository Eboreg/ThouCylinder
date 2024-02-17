package us.huseli.thoucylinder.dataclasses.combos

import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.entities.stripTitleCommons
import us.huseli.thoucylinder.dataclasses.youtube.YoutubePlaylist
import us.huseli.thoucylinder.dataclasses.youtube.YoutubeVideo
import us.huseli.thoucylinder.dataclasses.youtube.stripTitleCommons
import kotlin.math.max

data class YoutubePlaylistCombo(
    val playlist: YoutubePlaylist,
    val videos: List<YoutubeVideo>,
) {
    data class AlbumMatch(
        val score: Double,
        val albumCombo: AlbumWithTracksCombo,
        val playlistCombo: YoutubePlaylistCombo,
    )

    fun matchAlbumWithTracks(combo: AlbumWithTracksCombo): AlbumMatch =
        AlbumMatch(score = getAlbumDistance(combo), albumCombo = mergeWithAlbumCombo(combo), playlistCombo = this)

    suspend fun toAlbumCombo(isInLibrary: Boolean): AlbumWithTracksCombo {
        val album = Album(
            title = playlist.title,
            artist = playlist.artist,
            isInLibrary = isInLibrary,
            isLocal = false,
            youtubePlaylist = playlist,
            albumArt = playlist.getMediaStoreImage(),
        )

        return AlbumWithTracksCombo(
            album = album,
            tracks = videos.mapIndexed { index, video ->
                Track(
                    title = playlist.artist?.let {
                        video.title.replace(Regex("^$it (- )?", RegexOption.IGNORE_CASE), "")
                    } ?: video.title,
                    isInLibrary = isInLibrary,
                    albumId = album.albumId,
                    albumPosition = index + 1,
                    youtubeVideo = video,
                )
            }.stripTitleCommons(),
        )
    }

    private fun getAlbumDistance(combo: AlbumWithTracksCombo): Double {
        /**
         * Test if relevant strings from this release and its tracks are contained in the corresponding strings of
         * combo. Perfect match returns 0. If there are fewer videos in the playlist than there are tracks in the
         * album, the difference is factored into the result. The result is arbitrarily weighted.
         */
        var result = 0.0

        val videos = videos
            .stripTitleCommons()
            .mapIndexed { index, video ->
                // Remove any "[artist] - " strings in the video titles:
                (combo.tracks.getOrNull(index)?.artist ?: combo.album.artist)?.let {
                    video.copy(title = video.title.replace(Regex("^$it( - *)?", RegexOption.IGNORE_CASE), ""))
                } ?: video
            }
        // Strip "[artist] - " from playlist title too:
        val playlistTitle =
            combo.album.artist?.let { playlist.title.replace(Regex("^$it( - *)?", RegexOption.IGNORE_CASE), "") }
                ?: playlist.title
        // +1 for each missing or non-matching video:
        val videosTotal = videos
            .zip(combo.tracks)
            .filter { (video, track) -> !video.title.contains(track.title, true) }
            .size + max(combo.tracks.size - videos.size, 0)

        if (playlist.artist != null && combo.album.artist != null && playlist.artist != combo.album.artist) result++
        if (!playlistTitle.contains(combo.album.title, true)) result++
        if (combo.tracks.isNotEmpty()) result += (videosTotal.toDouble() / combo.tracks.size) * 2

        return result
    }

    private fun mergeWithAlbumCombo(combo: AlbumWithTracksCombo): AlbumWithTracksCombo {
        /**
         * We assume that checks have already been made so that the videos match the tracks (e.g. via
         * getAlbumDistance() above). Also, if there are fewer tracks on the album than videos in the playlist, surplus
         * videos will be discarded and vice versa.
         */
        return combo.copy(
            album = combo.album.copy(youtubePlaylist = playlist),
            tracks = combo.tracks.zip(videos).map { (track, video) ->
                track.copy(youtubeVideo = video)
            },
        )
    }
}
