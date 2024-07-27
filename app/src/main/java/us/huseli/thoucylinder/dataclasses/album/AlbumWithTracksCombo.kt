package us.huseli.thoucylinder.dataclasses.album

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import us.huseli.thoucylinder.dataclasses.artist.AlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.IAlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.joined
import us.huseli.thoucylinder.dataclasses.tag.AlbumTag
import us.huseli.thoucylinder.dataclasses.tag.Tag
import us.huseli.thoucylinder.dataclasses.track.ITrackCombo
import us.huseli.thoucylinder.dataclasses.track.Track
import us.huseli.thoucylinder.dataclasses.track.TrackCombo
import us.huseli.thoucylinder.dataclasses.track.withTracks
import kotlin.math.abs

enum class TrackMergeStrategy { KEEP_LEAST, KEEP_MOST, KEEP_SELF, KEEP_OTHER }


data class UnsavedAlbumWithTracksCombo(
    override val album: UnsavedAlbum,
    override val artists: List<IAlbumArtistCredit> = emptyList(),
    override val tags: List<Tag> = emptyList(),
    override val trackCombos: List<ITrackCombo> = emptyList(),
) : IAlbumWithTracksCombo<IAlbum>, IUnsavedAlbumCombo {
    data class AlbumMatch(
        val distance: Double,
        val albumCombo: UnsavedAlbumWithTracksCombo,
    )

    fun match(other: IAlbumWithTracksCombo<IAlbum>) =
        AlbumMatch(distance = getDistance(other), albumCombo = this)

    override fun updateWith(album: IAlbum?, tracks: List<Track>?) = copy(
        album = album?.asUnsavedAlbum() ?: this.album,
        trackCombos = tracks?.let { trackCombos.withTracks(it) } ?: trackCombos,
    )

    private fun getDistance(other: IAlbumWithTracksCombo<*>): Double {
        var result = 0.0

        // +1 if _none_ of the credited artists match:
        if (!artists.any { (other.artists.joined() ?: other.album.title).contains(it.name, true) }) result++
        if (!album.title.contains(other.album.title, true)) result++
        result += getTracksDistance(other.trackCombos.map { it.track })

        return result
    }

    private fun getTracksDistance(otherTracks: Collection<Track>): Double = otherTracks.takeIf { it.isNotEmpty() }
        ?.let { (getTracksDistanceSum(it) / otherTracks.size) * 2 }
        ?: 0.0

    private fun getTracksDistanceSum(otherTracks: Collection<Track>): Double = otherTracks.zip(trackCombos)
        .filter { (other, our) -> !other.title.contains(our.track.title, true) }
        .size.toDouble() + abs(trackCombos.size - otherTracks.size)
}


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
    override val tags: List<Tag> = emptyList(),
    @Relation(parentColumn = "Album_albumId", entityColumn = "AlbumArtist_albumId")
    override val artists: List<AlbumArtistCredit> = emptyList(),
    @Relation(parentColumn = "Album_albumId", entityColumn = "Track_albumId")
    override val trackCombos: List<TrackCombo> = emptyList<TrackCombo>().let { combos ->
        combos.map { it.copy(albumArtists = artists) }
    },
) : IAlbumWithTracksCombo<Album>, ISavedAlbumCombo {
    override fun updateWith(album: IAlbum?, tracks: List<Track>?) = copy(
        album = album?.asSavedAlbum() ?: this.album,
        trackCombos = tracks?.let { trackCombos.withTracks(it) } ?: trackCombos,
    )
}
