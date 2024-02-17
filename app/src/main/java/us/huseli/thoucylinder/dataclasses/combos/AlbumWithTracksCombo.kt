package us.huseli.thoucylinder.dataclasses.combos

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import us.huseli.retaintheme.extensions.sumOfOrNull
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.AlbumTag
import us.huseli.thoucylinder.dataclasses.entities.Tag
import us.huseli.thoucylinder.dataclasses.entities.Track

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
    @Relation(parentColumn = "Album_albumId", entityColumn = "Track_albumId")
    val tracks: List<Track> = emptyList(),
) : AbstractAlbumCombo() {
    val discCount: Int
        get() = tracks.mapNotNull { it.discNumber }.maxOrNull() ?: 1

    val trackCombos: List<TrackCombo>
        get() = tracks.map { TrackCombo(track = it, album = album) }

    override val trackCount: Int
        get() = tracks.size

    override val durationMs: Long?
        get() = tracks.sumOfOrNull {
            it.metadata?.durationMs ?: it.youtubeVideo?.durationMs ?: it.youtubeVideo?.metadata?.durationMs
        }

    override val minYear: Int?
        get() = tracks.mapNotNull { it.year }.minOrNull()

    override val maxYear: Int?
        get() = tracks.mapNotNull { it.year }.maxOrNull()

    override val isPartiallyDownloaded: Boolean
        get() = tracks.any { it.isDownloaded } && tracks.any { !it.isDownloaded }

    fun indexOfTrack(track: Track): Int = tracks.map { it.trackId }.indexOf(track.trackId)

    fun sorted(): AlbumWithTracksCombo = copy(
        tracks = tracks.sorted(),
        tags = tags.sortedBy { it.name.length },
    )

    override fun toString() = album.toString()
}
