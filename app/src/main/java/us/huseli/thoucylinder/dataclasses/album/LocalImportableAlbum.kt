package us.huseli.thoucylinder.dataclasses.album

import us.huseli.thoucylinder.dataclasses.ID3Data
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.artist.IAlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.UnsavedAlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.UnsavedTrackArtistCredit
import us.huseli.thoucylinder.dataclasses.track.Track
import us.huseli.thoucylinder.dataclasses.track.TrackMetadata
import us.huseli.thoucylinder.dataclasses.track.UnsavedTrackCombo
import us.huseli.thoucylinder.dataclasses.track.stripTitleCommons
import us.huseli.thoucylinder.enums.AlbumType
import us.huseli.thoucylinder.interfaces.IExternalAlbumWithTracks
import java.util.UUID
import kotlin.time.Duration

data class LocalImportableAlbum(
    override val id: String = UUID.randomUUID().toString(),
    override val title: String,
    override val artistName: String?,
    override val thumbnailUrl: String? = null,
    override val year: Int? = null,
    override val duration: Duration? = null,
    override val playCount: Int? = null,
    val musicBrainzReleaseId: String?,
    val musicBrainzReleaseGroupId: String?,
    val tracks: List<LocalImportableTrack> = emptyList(),
) : IExternalAlbumWithTracks {
    data class LocalImportableTrack(
        val title: String,
        val albumPosition: Int?,
        val metadata: TrackMetadata,
        val localUri: String,
        val id3: ID3Data,
    ) {
        fun toTrackCombo(album: UnsavedAlbum, albumArtists: List<IAlbumArtistCredit>): UnsavedTrackCombo {
            val track = Track(
                title = title,
                albumId = album.albumId,
                albumPosition = albumPosition,
                discNumber = id3.discNumber ?: 1,
                year = id3.year,
                localUri = localUri,
                musicBrainzId = id3.musicBrainzTrackId,
                durationMs = metadata.durationMs,
                metadata = metadata,
            )

            return UnsavedTrackCombo(
                track = track,
                album = album,
                trackArtists = id3.artist?.let {
                    listOf(UnsavedTrackArtistCredit(name = it, trackId = track.trackId))
                } ?: emptyList(),
                albumArtists = albumArtists,
            )
        }
    }

    override val albumType: AlbumType?
        get() = if (artistName?.lowercase() == "various artists") AlbumType.COMPILATION else null

    fun toAlbumWithTracks(
        base: AlbumCombo?,
        isLocal: Boolean = true,
        isInLibrary: Boolean = true,
        albumId: String? = null,
    ): UnsavedAlbumWithTracksCombo {
        val album = UnsavedAlbum(
            albumArt = thumbnailUrl?.let { MediaStoreImage(it) },
            albumId = base?.album?.albumId ?: albumId ?: id,
            albumType = albumType,
            isInLibrary = isInLibrary,
            isLocal = isLocal,
            musicBrainzReleaseGroupId = musicBrainzReleaseGroupId ?: base?.album?.musicBrainzReleaseGroupId,
            musicBrainzReleaseId = musicBrainzReleaseId ?: base?.album?.musicBrainzReleaseId,
            title = title,
            trackCount = trackCount,
            year = year ?: base?.album?.year,
        )
        val albumArtists = artistName
            ?.takeIf { it.lowercase() != "various artists" }
            ?.let { listOf(UnsavedAlbumArtistCredit(name = it, albumId = album.albumId)) }
            ?: base?.artists
            ?: emptyList()

        return UnsavedAlbumWithTracksCombo(
            album = album,
            artists = albumArtists,
            trackCombos = tracks.map {
                it.toTrackCombo(album = album, albumArtists = albumArtists)
            }.stripTitleCommons(),
        )
    }

    override fun toAlbumWithTracks(
        isLocal: Boolean,
        isInLibrary: Boolean,
        albumId: String?,
    ): UnsavedAlbumWithTracksCombo = toAlbumWithTracks(
        base = null,
        isLocal = isLocal,
        isInLibrary = isInLibrary,
        albumId = albumId,
    )

    override val trackCount: Int
        get() = tracks.size
}
