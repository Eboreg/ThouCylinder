package us.huseli.thoucylinder.dataclasses.combos

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.AlbumTag
import us.huseli.thoucylinder.dataclasses.entities.Tag


data class AlbumCombo(
    @Embedded override val album: Album,
    override val durationMs: Long? = null,
    override val minYear: Int? = null,
    override val maxYear: Int? = null,
    override val trackCount: Int = 0,
    override val isPartiallyDownloaded: Boolean = false,
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
) : AbstractAlbumCombo() {
    fun sortTags() = copy(tags = tags.sortedBy { it.name.length })

    override fun toString() = album.toString()
}

fun Collection<AlbumCombo>.sortTags() = map { it.sortTags() }
