package us.huseli.thoucylinder.dataclasses.combos

import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.views.AlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.youtube.YoutubePlaylist
import us.huseli.thoucylinder.dataclasses.youtube.YoutubeVideo
import us.huseli.thoucylinder.dataclasses.youtube.stripTitleCommons
import kotlin.math.max

data class YoutubePlaylistCombo(
    val playlist: YoutubePlaylist,
    val videos: List<YoutubeVideo>,
) {
    data class AlbumMatch(
        val distance: Double,
        val albumCombo: AlbumWithTracksCombo,
        val playlistCombo: YoutubePlaylistCombo,
    )

    fun matchAlbumWithTracks(combo: AlbumWithTracksCombo): AlbumMatch =
        AlbumMatch(distance = getAlbumDistance(combo), albumCombo = mergeWithAlbumCombo(combo), playlistCombo = this)

    suspend fun toAlbumCombo(isInLibrary: Boolean, getArtist: suspend (String) -> Artist): AlbumWithTracksCombo {
        val album = playlist.toAlbum(isInLibrary = isInLibrary)
        val albumArtist = playlist.artist?.let { getArtist(it) }
            ?.let { AlbumArtistCredit(artist = it, albumId = album.albumId) }
        val albumArtists = albumArtist?.let { listOf(it) } ?: emptyList()

        return AlbumWithTracksCombo(
            album = album,
            artists = albumArtists,
            trackCombos = videos.mapIndexed { index, video ->
                video.toTrackCombo(
                    isInLibrary = isInLibrary,
                    artist = playlist.artist,
                    album = album,
                    albumPosition = index + 1,
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
        val albumArtistString = combo.artists.joined()
        val trimTitle: (String, String?) -> String = { title, artistString ->
            artistString?.let { title.replace(Regex("^$it( - *)?", RegexOption.IGNORE_CASE), "") } ?: title
        }

        val videos = videos
            .stripTitleCommons()
            .mapIndexed { index, video ->
                // Remove any leading "[artist] - " strings in the video titles:
                val trackArtistString = combo.trackCombos.getOrNull(index)?.artists?.joined()
                video.copy(title = trimTitle(trimTitle(video.title, albumArtistString), trackArtistString))
            }
        // Strip leading "[artist] - " from playlist title too:
        val playlistTitle = trimTitle(playlist.title, albumArtistString)
        // +1 for each missing or non-matching video:
        val videosTotal = videos
            .zip(combo.trackCombos)
            .filter { (video, combo) -> !video.title.contains(combo.track.title, true) }
            .size + max(combo.trackCombos.size - videos.size, 0)

        if (playlist.artist != null && albumArtistString != null && playlist.artist != albumArtistString) result++
        if (!playlistTitle.contains(combo.album.title, true)) result++
        if (combo.trackCombos.isNotEmpty()) result += (videosTotal.toDouble() / combo.trackCombos.size) * 2

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
            trackCombos = combo.trackCombos.zip(videos).map { (trackCombo, video) ->
                trackCombo.copy(track = trackCombo.track.copy(youtubeVideo = video))
            },
        )
    }
}
