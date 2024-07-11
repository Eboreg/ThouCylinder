package us.huseli.thoucylinder.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName
import us.huseli.thoucylinder.dataclasses.album.IAlbum
import us.huseli.thoucylinder.dataclasses.track.Track
import us.huseli.thoucylinder.dataclasses.track.UnsavedTrackCombo
import us.huseli.thoucylinder.interfaces.IExternalTrack

abstract class AbstractMusicBrainzTrack : AbstractMusicBrainzItem() {
    abstract val length: Int
    abstract val number: String
}

data class MusicBrainzTrack(
    @SerializedName("artist-credit")
    val artistCredit: List<MusicBrainzArtistCredit>,
    override val id: String,
    override val length: Int,
    override val number: String,
    val position: Int,
    val recording: MusicBrainzRecording,
    override val title: String,
) : AbstractMusicBrainzTrack(), IExternalTrack {
    val artist: String?
        get() = artistCredit.joined().takeIf { it.isNotEmpty() }

    val year: Int?
        get() = recording.year

    override fun toTrackCombo(isInLibrary: Boolean, album: IAlbum?): UnsavedTrackCombo {
        val track = Track(
            musicBrainzId = recording.id,
            isInLibrary = isInLibrary,
            year = year,
            title = title,
            durationMs = length.toLong(),
            albumPosition = number.toIntOrNull(),
            albumId = album?.albumId,
        )

        return UnsavedTrackCombo(
            track = track,
            trackArtists = artistCredit.toNativeTrackArtists(trackId = track.trackId),
            album = album,
        )
    }
}
