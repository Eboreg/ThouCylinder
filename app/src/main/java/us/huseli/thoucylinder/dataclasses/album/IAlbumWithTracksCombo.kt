package us.huseli.thoucylinder.dataclasses.album

import us.huseli.thoucylinder.dataclasses.artist.IAlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.tag.AlbumTag
import us.huseli.thoucylinder.dataclasses.tag.Tag
import us.huseli.thoucylinder.dataclasses.tag.toAlbumTags
import us.huseli.thoucylinder.dataclasses.track.ITrackCombo
import us.huseli.thoucylinder.dataclasses.track.Track
import us.huseli.thoucylinder.dataclasses.track.UnsavedTrackCombo
import us.huseli.thoucylinder.enums.ListUpdateStrategy
import kotlin.math.max

interface IAlbumWithTracksCombo<out A : IAlbum> : IAlbumCombo<A> {
    val tags: List<Tag>
    val trackCombos: List<ITrackCombo>

    val albumTags: List<AlbumTag>
        get() = tags.toAlbumTags(album.albumId)

    val discCount: Int
        get() = trackCombos.mapNotNull { it.track.discNumber }.maxOrNull() ?: 1

    val tracks: List<Track>
        get() = trackCombos.map { it.track }

    val trackIds: List<String>
        get() = trackCombos.map { it.track.trackId }

    override val minYear: Int?
        get() = trackCombos.mapNotNull { it.track.year }.minOrNull()

    override val maxYear: Int?
        get() = trackCombos.mapNotNull { it.track.year }.maxOrNull()

    override val isPartiallyDownloaded: Boolean
        get() = trackCombos.any { it.track.isDownloaded } && trackCombos.any { !it.track.isDownloaded }

    override val unplayableTrackCount: Int
        get() = trackCombos.count { !it.track.isPlayable }

    override val isDownloadable: Boolean
        get() = trackCombos.any { it.track.isDownloadable }

    fun updateWith(
        other: UnsavedAlbumWithTracksCombo,
        trackMergeStrategy: TrackMergeStrategy,
        albumArtistUpdateStrategy: ListUpdateStrategy = ListUpdateStrategy.REPLACE,
        trackArtistUpdateStrategy: ListUpdateStrategy = ListUpdateStrategy.REPLACE,
        tagUpdateStrategy: ListUpdateStrategy = ListUpdateStrategy.MERGE,
    ): UnsavedAlbumWithTracksCombo {
        /**
         * Returns a copy of self, with all basic data changed to that of `other`. Nullable foreign keys and embedded
         * objects such as spotifyId and youtubePlaylist are taken from `other` if not null there, or otherwise kept.
         */
        val mergedAlbum = album.mergeWith(other.album)
        val mergedTrackCombos = mergeTrackCombos(
            other = other.trackCombos,
            album = mergedAlbum,
            mergeStrategy = trackMergeStrategy,
            artistUpdateStrategy = trackArtistUpdateStrategy,
        )

        return UnsavedAlbumWithTracksCombo(
            album = mergedAlbum.asUnsavedAlbum(),
            tags = mergeTags(other = other.tags, updateStrategy = tagUpdateStrategy),
            trackCombos = mergedTrackCombos,
            artists = mergeAlbumArtists(other = other.artists, updateStrategy = albumArtistUpdateStrategy),
        )
    }

    private fun mergeAlbumArtists(
        other: List<IAlbumArtistCredit>,
        updateStrategy: ListUpdateStrategy = ListUpdateStrategy.REPLACE,
    ): List<IAlbumArtistCredit> {
        val albumArtists = other.map { it.withAlbumId(albumId = album.albumId) }.toMutableSet()
        if (updateStrategy == ListUpdateStrategy.MERGE) albumArtists.addAll(artists)

        return albumArtists.toList()
    }

    private fun mergeTags(other: List<Tag>, updateStrategy: ListUpdateStrategy = ListUpdateStrategy.MERGE): List<Tag> {
        return when (updateStrategy) {
            ListUpdateStrategy.MERGE -> tags.toSet().plus(other).toList()
            ListUpdateStrategy.REPLACE -> other
        }
    }

    private fun mergeTrackCombos(
        other: List<ITrackCombo>,
        album: IAlbum,
        mergeStrategy: TrackMergeStrategy,
        artistUpdateStrategy: ListUpdateStrategy = ListUpdateStrategy.REPLACE,
    ): List<ITrackCombo> {
        val mergedTrackCombos = mutableListOf<ITrackCombo>()

        for (i in 0 until max(trackCombos.size, other.size)) {
            val thisTrackCombo = trackCombos.find { it.track.albumPosition == i + 1 }
            val otherTrackCombo = other.find { it.track.albumPosition == i + 1 }

            if (thisTrackCombo != null && otherTrackCombo != null) {
                val trackArtists = otherTrackCombo.trackArtists
                    .map { it.withTrackId(trackId = thisTrackCombo.track.trackId) }
                    .toMutableSet()
                if (artistUpdateStrategy == ListUpdateStrategy.MERGE) trackArtists.addAll(thisTrackCombo.trackArtists)

                mergedTrackCombos.add(
                    UnsavedTrackCombo(
                        track = otherTrackCombo.track.copy(
                            musicBrainzId = otherTrackCombo.track.musicBrainzId ?: thisTrackCombo.track.musicBrainzId,
                            trackId = thisTrackCombo.track.trackId,
                            albumId = thisTrackCombo.track.albumId,
                            localUri = otherTrackCombo.track.localUri ?: thisTrackCombo.track.localUri,
                            spotifyId = otherTrackCombo.track.spotifyId ?: thisTrackCombo.track.spotifyId,
                            youtubeVideo = otherTrackCombo.track.youtubeVideo ?: thisTrackCombo.track.youtubeVideo,
                            metadata = otherTrackCombo.track.metadata ?: thisTrackCombo.track.metadata,
                            image = otherTrackCombo.track.image ?: thisTrackCombo.track.image,
                            durationMs = otherTrackCombo.track.durationMs ?: thisTrackCombo.track.durationMs,
                        ),
                        album = album,
                        trackArtists = trackArtists.toList(),
                        albumArtists = otherTrackCombo.albumArtists,
                    )
                )
            } else if (
                thisTrackCombo != null &&
                (mergeStrategy == TrackMergeStrategy.KEEP_SELF || mergeStrategy == TrackMergeStrategy.KEEP_MOST)
            ) mergedTrackCombos.add(thisTrackCombo)
            else if (
                otherTrackCombo != null &&
                (mergeStrategy == TrackMergeStrategy.KEEP_OTHER || mergeStrategy == TrackMergeStrategy.KEEP_MOST)
            ) mergedTrackCombos.add(
                UnsavedTrackCombo(
                    track = otherTrackCombo.track,
                    album = album,
                    trackArtists = otherTrackCombo.trackArtists,
                    albumArtists = otherTrackCombo.albumArtists,
                )
            )
        }

        return mergedTrackCombos.toList()
    }
}
