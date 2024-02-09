package us.huseli.thoucylinder.dataclasses.combos

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import us.huseli.thoucylinder.dataclasses.entities.SpotifyArtist
import us.huseli.thoucylinder.dataclasses.entities.SpotifyTrack
import us.huseli.thoucylinder.dataclasses.entities.SpotifyTrackArtist
import us.huseli.thoucylinder.dataclasses.entities.Track
import kotlin.math.absoluteValue

data class SpotifyTrackCombo(
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

    fun hasSimilarDuration(track: Track, limit: Int = 3000): Boolean =
        (track.metadata?.durationMs ?: track.youtubeVideo?.durationMs)
            ?.let { (it - this.track.durationMs).absoluteValue <= limit } ?: false
}
