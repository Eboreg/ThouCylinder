package us.huseli.thoucylinder.dataclasses.pojos

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import us.huseli.thoucylinder.dataclasses.entities.SpotifyArtist
import us.huseli.thoucylinder.dataclasses.entities.SpotifyTrack
import us.huseli.thoucylinder.dataclasses.entities.SpotifyTrackArtist
import us.huseli.thoucylinder.dataclasses.entities.Track
import java.util.UUID

data class SpotifyTrackPojo(
    @Embedded val track: SpotifyTrack,
    @Relation(
        entity = SpotifyArtist::class,
        parentColumn = "SpotifyTrack_id",
        entityColumn = "SpotifyArtist_id",
        associateBy = Junction(
            value = SpotifyTrackArtist::class,
            parentColumn = "SpotifyTrackArtist_trackId",
            entityColumn = "SpotifyTrackArtist_artistId",
        )
    )
    val artists: List<SpotifyArtist>,
) {
    val artist: String?
        get() = artists.map { it.name }.takeIf { it.isNotEmpty() }?.joinToString("/")

    fun toTrack(isInLibrary: Boolean, albumId: UUID? = null) = Track(
        isInLibrary = isInLibrary,
        artist = artist,
        albumId = albumId,
        albumPosition = track.trackNumber,
        title = track.name,
    )
}
