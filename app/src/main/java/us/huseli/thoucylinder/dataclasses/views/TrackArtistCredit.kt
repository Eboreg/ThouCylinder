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
    SELECT TrackArtist.*,
        Artist_name AS TrackArtist_name,
        Artist_spotifyId AS TrackArtist_spotifyId,
        Artist_musicBrainzId AS TrackArtist_musicBrainzId,
        Artist_image_fullUriString AS TrackArtist_image_fullUriString,
        Artist_image_hash AS TrackArtist_image_hash,
        Artist_image_thumbnailUriString AS TrackArtist_image_thumbnailUriString
    FROM TrackArtist JOIN Artist ON TrackArtist_artistId = Artist_id
    ORDER BY TrackArtist_position
    """
)
data class TrackArtistCredit(
    @ColumnInfo("TrackArtist_trackId") val trackId: String,
    @ColumnInfo("TrackArtist_artistId") override val artistId: String = UUID.randomUUID().toString(),
    @ColumnInfo("TrackArtist_name") override val name: String,
    @ColumnInfo("TrackArtist_spotifyId") override val spotifyId: String? = null,
    @ColumnInfo("TrackArtist_musicBrainzId") override val musicBrainzId: String? = null,
    @ColumnInfo("TrackArtist_joinPhrase") override val joinPhrase: String = "/",
    @Embedded("TrackArtist_image_") override val image: MediaStoreImage? = null,
    @ColumnInfo("TrackArtist_position") override val position: Int = 0,
) : AbstractArtistCredit() {
    constructor(artist: Artist, trackId: String, joinPhrase: String = "/", position: Int = 0) : this(
        trackId = trackId,
        artistId = artist.artistId,
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
