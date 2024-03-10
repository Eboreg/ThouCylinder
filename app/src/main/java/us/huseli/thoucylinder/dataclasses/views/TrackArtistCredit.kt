package us.huseli.thoucylinder.dataclasses.views

import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Embedded
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.abstr.AbstractArtistCredit
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.entities.TrackArtist
import java.util.UUID

@DatabaseView(
    """
    SELECT TrackArtist.*, Artist_name AS TrackArtist_name, Artist_spotifyId AS TrackArtist_spotifyId,
        Artist_musicBrainzId AS TrackArtist_musicBrainzId, Artist_image_uri AS TrackArtist_image_uri,
        Artist_image_hash AS TrackArtist_image_hash, Artist_image_thumbnailUri AS TrackArtist_image_thumbnailUri
    FROM TrackArtist JOIN Artist ON TrackArtist_artistId = Artist_id
    ORDER BY TrackArtist_position
    """
)
data class TrackArtistCredit(
    @ColumnInfo("TrackArtist_trackId") val trackId: UUID,
    @ColumnInfo("TrackArtist_artistId") override val artistId: UUID = UUID.randomUUID(),
    @ColumnInfo("TrackArtist_name") override val name: String,
    @ColumnInfo("TrackArtist_spotifyId") override val spotifyId: String? = null,
    @ColumnInfo("TrackArtist_musicBrainzId") override val musicBrainzId: String? = null,
    @ColumnInfo("TrackArtist_joinPhrase") override val joinPhrase: String = "/",
    @Embedded("TrackArtist_image_") override val image: MediaStoreImage? = null,
    @ColumnInfo("TrackArtist_position") override val position: Int = 0,
) : AbstractArtistCredit() {
    constructor(artist: Artist, trackId: UUID, joinPhrase: String = "/", position: Int = 0) : this(
        trackId = trackId,
        artistId = artist.id,
        name = artist.name,
        spotifyId = artist.spotifyId,
        musicBrainzId = artist.musicBrainzId,
        image = artist.image,
        joinPhrase = joinPhrase,
        position = position,
    )

    constructor(artist: Artist, trackArtist: TrackArtist) : this(
        artist = artist,
        trackId = trackArtist.trackId,
        joinPhrase = trackArtist.joinPhrase,
        position = trackArtist.position,
    )

    fun toTrackArtist() =
        TrackArtist(trackId = trackId, artistId = artistId, joinPhrase = joinPhrase, position = position)
}

fun Iterable<TrackArtistCredit>.toTrackArtists(): Collection<TrackArtist> = map { it.toTrackArtist() }.toSet()
