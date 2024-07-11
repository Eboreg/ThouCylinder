package us.huseli.thoucylinder.dataclasses.artist

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Embedded
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import java.util.UUID

interface ITrackArtistCredit : IArtistCredit {
    val trackId: String

    override fun withSpotifyId(spotifyId: String): ITrackArtistCredit
    fun withTrackId(trackId: String): ITrackArtistCredit

    fun withArtistId(artistId: String) = TrackArtistCredit(
        trackId = trackId,
        artistId = artistId,
        name = name,
        spotifyId = spotifyId,
        musicBrainzId = musicBrainzId,
        joinPhrase = joinPhrase,
        image = image,
        position = position,
    )
}

@Immutable
data class UnsavedTrackArtistCredit(
    override val name: String,
    override val spotifyId: String? = null,
    override val musicBrainzId: String? = null,
    override val image: MediaStoreImage? = null,
    override val joinPhrase: String = "/",
    override val position: Int = 0,
    override val trackId: String,
) : ITrackArtistCredit {
    override fun withSpotifyId(spotifyId: String): UnsavedTrackArtistCredit = copy(spotifyId = spotifyId)
    override fun withTrackId(trackId: String): UnsavedTrackArtistCredit = copy(trackId = trackId)
}

@DatabaseView(
    """
    SELECT TrackArtist.*,
        Artist_name AS TrackArtist_name,
        Artist_spotifyId AS TrackArtist_spotifyId,
        Artist_musicBrainzId AS TrackArtist_musicBrainzId,
        Artist_image_fullUriString AS TrackArtist_image_fullUriString,
        Artist_image_thumbnailUriString AS TrackArtist_image_thumbnailUriString
    FROM TrackArtist JOIN Artist ON TrackArtist_artistId = Artist_id
    ORDER BY TrackArtist_position
    """
)
@Immutable
data class TrackArtistCredit(
    @ColumnInfo("TrackArtist_trackId") override val trackId: String,
    @ColumnInfo("TrackArtist_artistId") override val artistId: String = UUID.randomUUID().toString(),
    @ColumnInfo("TrackArtist_name") override val name: String,
    @ColumnInfo("TrackArtist_spotifyId") override val spotifyId: String? = null,
    @ColumnInfo("TrackArtist_musicBrainzId") override val musicBrainzId: String? = null,
    @ColumnInfo("TrackArtist_joinPhrase") override val joinPhrase: String = "/",
    @Embedded("TrackArtist_image_") override val image: MediaStoreImage? = null,
    @ColumnInfo("TrackArtist_position") override val position: Int = 0,
) : ITrackArtistCredit, ISavedArtistCredit {
    override fun withSpotifyId(spotifyId: String): TrackArtistCredit = copy(spotifyId = spotifyId)
    override fun withTrackId(trackId: String): TrackArtistCredit = copy(trackId = trackId)
}

fun Iterable<TrackArtistCredit>.toTrackArtists(): List<TrackArtist> = map {
    TrackArtist(
        trackId = it.trackId,
        artistId = it.artistId,
        joinPhrase = it.joinPhrase,
        position = it.position,
    )
}