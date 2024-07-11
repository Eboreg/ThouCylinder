package us.huseli.thoucylinder.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName
import us.huseli.thoucylinder.dataclasses.album.IAlbum
import us.huseli.thoucylinder.dataclasses.track.Track
import us.huseli.thoucylinder.dataclasses.track.UnsavedTrackCombo

abstract class AbstractMusicBrainzMedia {
    abstract val format: String?
    abstract val trackCount: Int
}

data class MusicBrainzSimplifiedMedia(
    override val format: String?,
    @SerializedName("track-count")
    override val trackCount: Int,
) : AbstractMusicBrainzMedia()

data class MusicBrainzMedia(
    override val format: String?,
    @SerializedName("track-count")
    override val trackCount: Int,
    val position: Int,
    @SerializedName("track-offset")
    val trackOffset: Int,
    val tracks: List<MusicBrainzTrack>,
) : AbstractMusicBrainzMedia()

fun Iterable<MusicBrainzMedia>.toTrackCombos(
    isInLibrary: Boolean,
    album: IAlbum? = null,
): List<UnsavedTrackCombo> = flatMap { medium ->
    medium.tracks.map { mbTrack ->
        val track = Track(
            title = mbTrack.title,
            isInLibrary = isInLibrary,
            albumPosition = mbTrack.position,
            discNumber = medium.position,
            year = mbTrack.year,
            musicBrainzId = mbTrack.recording.id,
            albumId = album?.albumId,
        )
        val artists = mbTrack.artistCredit.toNativeTrackArtists(trackId = track.trackId)

        UnsavedTrackCombo(track = track, trackArtists = artists, album = album)
    }
}
