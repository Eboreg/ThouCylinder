package us.huseli.thoucylinder.dataclasses.musicBrainz

import com.google.gson.annotations.SerializedName
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.BaseArtist
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.views.TrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.interfaces.IExternalAlbum

@Suppress("unused")
enum class MusicBrainzReleaseStatus {
    @SerializedName("Official") OFFICIAL,
    @SerializedName("Promotion") PROMOTION,
    @SerializedName("Bootleg") BOOTLEG,
    @SerializedName("Pseudo-release") PSEUDO_RELEASE,
    @SerializedName("Withdrawn") WITHDRAWN,
    @SerializedName("Cancelled") CANCELLED,
}

data class MusicBrainzRelease(
    @SerializedName("artist-credit")
    val artistCredit: List<MusicBrainzArtistCredit>,
    val country: String?,
    val date: String?,
    val genres: List<MusicBrainzGenre>,
    override val id: String,
    val media: List<Media>,
    val packaging: String?,
    @SerializedName("packaging-id")
    val packagingId: String?,
    val quality: String?,
    @SerializedName("release-group")
    val releaseGroup: ReleaseGroup,
    val status: MusicBrainzReleaseStatus?,
    @SerializedName("status-id")
    val statusId: String?,
    override val title: String,
) : AbstractMusicBrainzItem(), IExternalAlbum {
    data class ReleaseGroup(
        @SerializedName("artist-credit")
        val artistCredit: List<MusicBrainzArtistCredit>,
        val disambiguation: String?,
        @SerializedName("first-release-date")
        val firstReleaseDate: String?,
        val genres: List<MusicBrainzGenre>,
        override val id: String,
        @SerializedName("primary-type")
        val primaryType: MusicBrainzReleaseGroupPrimaryType?,
        @SerializedName("primary-type-id")
        val primaryTypeId: String?,
        val title: String,
    ) : AbstractMusicBrainzItem() {
        val year: Int?
            get() = firstReleaseDate
                ?.substringBefore('-')
                ?.takeIf { it.matches(Regex("^\\d{4}$")) }
                ?.toInt()
    }

    data class Media(
        val format: String?,
        @SerializedName("format-id")
        val formatId: String?,
        val position: Int,
        @SerializedName("track-count")
        val trackCount: Int,
        @SerializedName("track-offset")
        val trackOffset: Int,
        val tracks: List<MusicBrainzTrack>,
    )

    private val allGenres: List<MusicBrainzGenre>
        get() = genres
            .asSequence()
            .plus(releaseGroup.genres)
            .plus(media.flatMap { media -> media.tracks.flatMap { track -> track.recording.genres } })
            .groupBy { it }
            .map { (genre, instances) -> genre.copy(count = instances.sumOf { it.count }) }
            .sortedByDescending { it.count }

    val artist: String
        get() = artistCredit.joined()

    override val artistName: String
        get() = artist

    val year: Int?
        get() = releaseGroup.year ?: date
            ?.substringBefore('-')
            ?.takeIf { it.matches(Regex("^\\d{4}$")) }
            ?.toInt()

    suspend fun toAlbumWithTracks(
        isInLibrary: Boolean,
        isLocal: Boolean = false,
        albumArt: MediaStoreImage? = null,
        getArtist: suspend (BaseArtist) -> Artist,
    ): AlbumWithTracksCombo {
        val album = Album(
            title = title,
            isInLibrary = isInLibrary,
            isLocal = isLocal,
            year = year,
            musicBrainzReleaseId = id,
            musicBrainzReleaseGroupId = releaseGroup.id,
            albumArt = albumArt,
        )
        val albumArtists = artistCredit.mapIndexed { index, albumArtist ->
            albumArtist.toNativeAlbumArtist(
                artist = getArtist(BaseArtist(name = albumArtist.name, musicBrainzId = albumArtist.artist.id)),
                albumId = album.albumId,
                position = index,
            )
        }

        return AlbumWithTracksCombo(
            album = album,
            tags = allGenres.toInternal(),
            artists = albumArtists,
            trackCombos = media.toTrackCombos(album = album, getArtist = getArtist, isInLibrary = isInLibrary),
        )
    }

    private suspend fun Iterable<Media>.toTrackCombos(
        isInLibrary: Boolean,
        album: Album? = null,
        getArtist: suspend (BaseArtist) -> Artist,
    ): List<TrackCombo> = flatMap { medium ->
        medium.tracks.map {
            val track = Track(
                title = it.title,
                isInLibrary = isInLibrary,
                albumPosition = it.position,
                discNumber = medium.position,
                year = it.year,
                musicBrainzId = it.id,
                albumId = album?.albumId,
                durationMs = it.length.toLong(),
            )
            val artists = it.artistCredit.mapIndexed { index, trackArtist ->
                trackArtist.toNativeTrackArtist(
                    artist = getArtist(BaseArtist(name = trackArtist.name, musicBrainzId = trackArtist.artist.id)),
                    trackId = track.trackId,
                    position = index,
                )
            }

            TrackCombo(track = track, artists = artists, album = album)
        }
    }
}
