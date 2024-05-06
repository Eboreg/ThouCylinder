package us.huseli.thoucylinder.dataclasses.combos

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import us.huseli.retaintheme.extensions.sum
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.UnsavedArtist
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.views.AlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.views.stripTitleCommons
import us.huseli.thoucylinder.dataclasses.youtube.YoutubePlaylist
import us.huseli.thoucylinder.dataclasses.youtube.YoutubeVideo
import us.huseli.thoucylinder.dataclasses.youtube.stripTitleCommons
import us.huseli.thoucylinder.interfaces.IExternalAlbum
import kotlin.math.max
import kotlin.time.Duration

@Immutable
data class YoutubePlaylistCombo(val playlist: YoutubePlaylist, val videos: ImmutableList<YoutubeVideo>) :
    IExternalAlbum {
    data class AlbumMatch(
        val distance: Double,
        val albumCombo: AlbumWithTracksCombo,
        val playlistCombo: YoutubePlaylistCombo,
    )

    override val id: String
        get() = playlist.id

    override val title: String
        get() = playlist.title

    override val artistName: String?
        get() = playlist.artist

    override val thumbnailUrl: String?
        get() = playlist.thumbnailUrl

    override val trackCount: Int
        get() = videos.size

    override val year: Int?
        get() = null

    override val duration: Duration
        get() = videos.mapNotNull { it.duration }.sum()

    override val playCount: Int?
        get() = null

    override suspend fun getMediaStoreImage(): MediaStoreImage? = playlist.getMediaStoreImage()

    override suspend fun toAlbumWithTracks(
        isLocal: Boolean,
        isInLibrary: Boolean,
        getArtist: suspend (UnsavedArtist) -> Artist,
    ): AlbumWithTracksCombo {
        val album = playlist.toAlbum(isInLibrary = isInLibrary, isLocal = isLocal)
        val artist = playlist.artist?.let { getArtist(UnsavedArtist(name = it)) }
        val albumArtist = artist?.let { AlbumArtistCredit(artist = it, albumId = album.albumId) }

        return AlbumWithTracksCombo(
            album = album,
            artists = albumArtist?.let { listOf(it) } ?: emptyList(),
            trackCombos = videos.mapIndexed { index, video ->
                video.toTrackCombo(
                    isInLibrary = isInLibrary,
                    albumArtist = artist,
                    album = album,
                    albumPosition = index + 1,
                )
            }.stripTitleCommons(),
        )
    }

    fun matchAlbumWithTracks(combo: AlbumWithTracksCombo): AlbumMatch =
        AlbumMatch(distance = getAlbumDistance(combo), albumCombo = mergeWithAlbumCombo(combo), playlistCombo = this)

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
