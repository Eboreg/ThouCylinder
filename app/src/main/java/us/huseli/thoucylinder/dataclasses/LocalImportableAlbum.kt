package us.huseli.thoucylinder.dataclasses

import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.views.AlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.views.AlbumCombo
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit
import us.huseli.thoucylinder.dataclasses.views.TrackCombo
import us.huseli.thoucylinder.dataclasses.views.stripTitleCommons
import us.huseli.thoucylinder.interfaces.IExternalAlbum
import java.util.UUID
import kotlin.time.Duration

data class LocalImportableAlbum(
    override val id: String = UUID.randomUUID().toString(),
    override val title: String,
    override val artistName: String?,
    override val thumbnailUrl: String? = null,
    override val trackCount: Int? = null,
    override val year: Int? = null,
    override val duration: Duration? = null,
    override val playCount: Int? = null,
    val musicBrainzReleaseId: String?,
    val musicBrainzReleaseGroupId: String?,
    val tracks: List<LocalImportableTrack> = emptyList(),
) : IExternalAlbum {
    data class LocalImportableTrack(
        val title: String,
        val albumPosition: Int?,
        val metadata: TrackMetadata,
        val localUri: String,
        val id3: ID3Data,
    ) {
        suspend fun toTrackCombo(
            album: Album,
            albumArtists: List<AlbumArtistCredit>,
            getArtist: suspend (UnsavedArtist) -> Artist,
        ): TrackCombo {
            val track = Track(
                title = title,
                albumId = album.albumId,
                albumPosition = albumPosition,
                discNumber = id3.discNumber,
                year = id3.year,
                localUri = localUri,
                musicBrainzId = id3.musicBrainzTrackId,
                durationMs = metadata.durationMs,
                metadata = metadata,
            )

            return TrackCombo(
                track = track,
                album = album,
                artists = id3.artist?.let {
                    listOf(
                        TrackArtistCredit(
                            artist = getArtist(UnsavedArtist(name = it)),
                            trackId = track.trackId
                        )
                    )
                } ?: emptyList(),
                albumArtists = albumArtists,
            )
        }
    }

    suspend fun toAlbumWithTracks(
        base: AlbumCombo?,
        getArtist: suspend (UnsavedArtist) -> Artist,
        isLocal: Boolean = true,
        isInLibrary: Boolean = true,
    ): AlbumWithTracksCombo {
        val album = Album(
            albumId = base?.album?.albumId ?: id,
            year = year ?: base?.album?.year,
            title = title,
            musicBrainzReleaseId = musicBrainzReleaseId ?: base?.album?.musicBrainzReleaseId,
            musicBrainzReleaseGroupId = musicBrainzReleaseGroupId ?: base?.album?.musicBrainzReleaseGroupId,
            isLocal = isLocal,
            isInLibrary = isInLibrary,
            albumArt = thumbnailUrl?.let { MediaStoreImage(it) },
        )
        val albumArtists = artistName
            ?.let { listOf(AlbumArtistCredit(artist = getArtist(UnsavedArtist(name = it)), albumId = album.albumId)) }
            ?: base?.artists
            ?: emptyList()

        return AlbumWithTracksCombo(
            album = album,
            artists = albumArtists,
            trackCombos = tracks.map {
                it.toTrackCombo(album = album, albumArtists = albumArtists, getArtist = getArtist)
            }.stripTitleCommons(),
        )
    }

    override suspend fun toAlbumWithTracks(
        isLocal: Boolean,
        isInLibrary: Boolean,
        getArtist: suspend (UnsavedArtist) -> Artist,
    ): AlbumWithTracksCombo = toAlbumWithTracks(
        base = null,
        getArtist = getArtist,
        isLocal = isLocal,
        isInLibrary = isInLibrary,
    )
}
