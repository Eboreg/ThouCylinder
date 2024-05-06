package us.huseli.thoucylinder.dataclasses.combos

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import us.huseli.retaintheme.extensions.sumOfOrNull
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.AlbumTag
import us.huseli.thoucylinder.dataclasses.entities.Tag
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.entities.toAlbumTags
import us.huseli.thoucylinder.dataclasses.views.AlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.views.TrackCombo
import kotlin.math.abs
import kotlin.math.max

enum class TrackMergeStrategy { KEEP_LEAST, KEEP_MOST, KEEP_SELF, KEEP_OTHER }

data class AlbumWithTracksCombo(
    @Embedded override val album: Album,
    @Relation(
        entity = Tag::class,
        parentColumn = "Album_albumId",
        entityColumn = "Tag_name",
        associateBy = Junction(
            value = AlbumTag::class,
            parentColumn = "AlbumTag_albumId",
            entityColumn = "AlbumTag_tagName",
        )
    )
    val tags: List<Tag> = emptyList(),
    @Relation(parentColumn = "Album_albumId", entityColumn = "AlbumArtist_albumId")
    override val artists: List<AlbumArtistCredit> = emptyList(),
    @Relation(parentColumn = "Album_albumId", entityColumn = "Track_albumId")
    val trackCombos: List<TrackCombo> = emptyList<TrackCombo>().let { combos ->
        combos.map { it.copy(albumArtists = artists) }
    },
) : AbstractAlbumCombo() {
    data class AlbumMatch(
        val distance: Double,
        val albumCombo: AlbumWithTracksCombo,
    )

    val albumTags: List<AlbumTag>
        get() = tags.toAlbumTags(album.albumId)

    val discCount: Int
        get() = trackCombos.mapNotNull { it.track.discNumber }.maxOrNull() ?: 1

    val tracks: List<Track>
        get() = trackCombos.map { it.track }

    val trackIds: List<String>
        get() = trackCombos.map { it.track.trackId }

    override val trackCount: Int
        get() = trackCombos.size

    override val minYear: Int?
        get() = trackCombos.mapNotNull { it.track.year }.minOrNull()

    override val maxYear: Int?
        get() = trackCombos.mapNotNull { it.track.year }.maxOrNull()

    override val isPartiallyDownloaded: Boolean
        get() = trackCombos.any { it.track.isDownloaded } && trackCombos.any { !it.track.isDownloaded }

    override val durationMs: Long?
        get() = trackCombos.sumOfOrNull { it.track.durationMs }

    override val unplayableTrackCount: Int
        get() = trackCombos.count { !it.track.isPlayable }

    private fun getDistance(other: AlbumWithTracksCombo): Double {
        var result = 0.0

        // +1 if _none_ of the credited artists match:
        if (!artists.any { (other.artists.joined() ?: other.album.title).contains(it.name, true) }) result++
        if (!album.title.contains(other.album.title, true)) result++
        result += getTracksDistance(other.trackCombos.map { it.track })

        return result
    }

    fun match(other: AlbumWithTracksCombo) = AlbumMatch(distance = getDistance(other), albumCombo = this)

    fun updateWith(other: AlbumWithTracksCombo, strategy: TrackMergeStrategy): AlbumWithTracksCombo {
        /**
         * Returns a copy of self, with all basic data changed to that of `other`. Nullable foreign keys and embedded
         * objects such as spotifyId and youtubePlaylist are taken from `other` if not null there, or otherwise kept.
         */
        val mergedAlbum = other.album.copy(
            musicBrainzReleaseId = other.album.musicBrainzReleaseId ?: album.musicBrainzReleaseId,
            musicBrainzReleaseGroupId = other.album.musicBrainzReleaseGroupId ?: album.musicBrainzReleaseGroupId,
            albumId = album.albumId,
            spotifyId = other.album.spotifyId ?: album.spotifyId,
            youtubePlaylist = other.album.youtubePlaylist ?: album.youtubePlaylist,
            albumArt = other.album.albumArt ?: album.albumArt,
            spotifyImage = other.album.spotifyImage ?: album.spotifyImage,
        )
        val mergedTrackCombos = mutableListOf<TrackCombo>()

        for (i in 0 until max(trackCombos.size, other.trackCombos.size)) {
            val thisTrackCombo = trackCombos.getOrNull(i)
            val otherTrackCombo = other.trackCombos.getOrNull(i)

            if (thisTrackCombo != null && otherTrackCombo != null) {
                mergedTrackCombos.add(
                    otherTrackCombo.copy(
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
                        album = mergedAlbum,
                        artists = otherTrackCombo.artists.map { it.copy(trackId = thisTrackCombo.track.trackId) },
                    )
                )
            } else if (
                thisTrackCombo != null &&
                (strategy == TrackMergeStrategy.KEEP_SELF || strategy == TrackMergeStrategy.KEEP_MOST)
            ) mergedTrackCombos.add(thisTrackCombo)
            else if (
                otherTrackCombo != null &&
                (strategy == TrackMergeStrategy.KEEP_OTHER || strategy == TrackMergeStrategy.KEEP_MOST)
            ) mergedTrackCombos.add(otherTrackCombo.copy(album = mergedAlbum))
        }

        return AlbumWithTracksCombo(
            album = mergedAlbum,
            tags = tags.toSet().plus(other.tags).toList(),
            trackCombos = mergedTrackCombos,
            artists = other.artists.map { it.copy(albumId = album.albumId) },
        )
    }

    private fun getTracksDistance(otherTracks: Collection<Track>): Double = otherTracks.takeIf { it.isNotEmpty() }
        ?.let { (getTracksDistanceSum(it) / otherTracks.size) * 2 }
        ?: 0.0

    private fun getTracksDistanceSum(otherTracks: Collection<Track>): Double = otherTracks.zip(trackCombos)
        .filter { (other, our) -> !other.title.contains(our.track.title, true) }
        .size.toDouble() + abs(trackCombos.size - otherTracks.size)
}
