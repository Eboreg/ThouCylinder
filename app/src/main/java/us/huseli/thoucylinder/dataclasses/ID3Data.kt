package us.huseli.thoucylinder.dataclasses

import androidx.compose.runtime.Immutable
import com.arthenica.ffmpegkit.MediaInformation
import us.huseli.retaintheme.extensions.filterValuesNotNull
import us.huseli.thoucylinder.dataclasses.album.IAlbum
import us.huseli.thoucylinder.dataclasses.artist.IArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.joined
import us.huseli.thoucylinder.dataclasses.track.Track
import us.huseli.thoucylinder.getIntOrNull
import us.huseli.thoucylinder.getStringOrNull

@Immutable
data class ID3Data(
    val album: String? = null,
    val albumArtist: String? = null,
    val artist: String? = null,
    val discNumber: Int? = null,
    val musicBrainzAlbumArtistId: String? = null,
    val musicBrainzArtistId: String? = null,
    val musicBrainzReleaseGroupId: String? = null,
    val musicBrainzReleaseId: String? = null,
    val musicBrainzTrackId: String? = null,
    val title: String? = null,
    val trackNumber: Int? = null,
    val year: Int? = null,
) {
    fun toTagMap(): Map<String, String> = mapOf(
        "album" to album,
        "album_artist" to albumArtist,
        "artist" to artist,
        "date" to year?.toString(),
        "disc" to discNumber?.toString(),
        "musicbrainz_albumartistid" to musicBrainzAlbumArtistId,
        "musicbrainz_albumid" to musicBrainzReleaseId,
        "musicbrainz_artistid" to musicBrainzArtistId,
        "musicbrainz_releasegroupid" to musicBrainzReleaseGroupId,
        "musicbrainz_trackid" to musicBrainzTrackId,
        "title" to title,
        "track" to trackNumber?.toString(),
    ).filterValuesNotNull()

    companion object {
        fun fromTrack(
            track: Track,
            trackArtists: Collection<IArtistCredit>,
            album: IAlbum?,
            albumArtists: Collection<IArtistCredit>? = null,
        ) = ID3Data(
            album = album?.title,
            albumArtist = albumArtists?.joined(),
            artist = trackArtists.joined() ?: albumArtists?.joined(),
            discNumber = track.discNumber,
            musicBrainzAlbumArtistId = albumArtists?.firstNotNullOfOrNull { it.musicBrainzId },
            musicBrainzArtistId = trackArtists.firstNotNullOfOrNull { it.musicBrainzId },
            musicBrainzReleaseGroupId = album?.musicBrainzReleaseGroupId,
            musicBrainzReleaseId = album?.musicBrainzReleaseId,
            musicBrainzTrackId = track.musicBrainzId,
            title = track.title,
            trackNumber = track.albumPosition,
            year = track.year ?: album?.year,
        )
    }
}

fun MediaInformation.extractID3Data(): ID3Data {
    val tags = this.tags ?: streams.firstNotNullOfOrNull { it.tags }

    return ID3Data(
        album = tags?.getStringOrNull("album"),
        albumArtist = tags?.getStringOrNull("album_artist"),
        artist = tags?.getStringOrNull("artist"),
        discNumber = tags?.getIntOrNull("disc"),
        musicBrainzAlbumArtistId = tags?.getStringOrNull("musicbrainz_albumartistid"),
        musicBrainzArtistId = tags?.getStringOrNull("musicbrainz_artistid"),
        musicBrainzReleaseGroupId = tags?.getStringOrNull("musicbrainz_releasegroupid"),
        musicBrainzReleaseId = tags?.getStringOrNull("musicbrainz_albumid"),
        musicBrainzTrackId = tags?.getStringOrNull("musicbrainz_trackid"),
        title = tags?.getStringOrNull("title"),
        trackNumber = tags?.getIntOrNull("track"),
        year = tags?.getIntOrNull("date")?.takeIf { it in 1000..3000 },
    )
}
