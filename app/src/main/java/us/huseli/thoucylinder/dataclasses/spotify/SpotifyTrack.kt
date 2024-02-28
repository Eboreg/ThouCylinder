package us.huseli.thoucylinder.dataclasses.spotify

import com.google.gson.annotations.SerializedName
import us.huseli.thoucylinder.dataclasses.combos.TrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.interfaces.IExternalTrack
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit
import java.util.UUID

abstract class AbstractSpotifyTrack<AT : AbstractSpotifyArtist> : AbstractSpotifyItem(), IExternalTrack {
    abstract val discNumber: Int
    abstract val durationMs: Int
    abstract val href: String?
    abstract override val id: String
    abstract val name: String
    abstract val trackNumber: Int
    abstract val uri: String?
    abstract val artists: List<AT>

    val artist: String
        get() = artists.artistString()

    override val title: String
        get() = name

    suspend fun toTrackCombo(getArtist: suspend (String) -> Artist, album: Album?): TrackCombo {
        val track = toTrack(album?.albumId)

        return TrackCombo(
            track = track,
            album = album,
            artists = artists.mapIndexed { index, artist ->
                TrackArtistCredit(artist = getArtist(artist.name), trackId = track.trackId)
                    .copy(spotifyId = artist.id, position = index)
            },
        )
    }

    private fun toTrack(albumId: UUID?) = Track(
        albumId = albumId,
        albumPosition = trackNumber,
        discNumber = discNumber,
        spotifyId = id,
        title = name,
    )
}

data class SpotifySimplifiedTrack(
    override val artists: List<SpotifySimplifiedArtist>,
    @SerializedName("disc_number")
    override val discNumber: Int,
    @SerializedName("duration_ms")
    override val durationMs: Int,
    override val href: String?,
    override val id: String,
    override val name: String,
    @SerializedName("track_number")
    override val trackNumber: Int,
    override val uri: String?,
) : AbstractSpotifyTrack<SpotifySimplifiedArtist>()

data class SpotifyTrack(
    override val artists: List<SpotifyArtist>,
    @SerializedName("disc_number")
    override val discNumber: Int,
    @SerializedName("duration_ms")
    override val durationMs: Int,
    override val href: String?,
    override val id: String,
    override val name: String,
    @SerializedName("track_number")
    override val trackNumber: Int,
    override val uri: String?,
    val album: SpotifySimplifiedAlbum,
    val popularity: Int,
) : AbstractSpotifyTrack<SpotifyArtist>()
