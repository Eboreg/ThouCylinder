package us.huseli.thoucylinder.dataclasses.tag

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity
@Immutable
data class Tag(
    @ColumnInfo("Tag_name") @PrimaryKey val name: String,
    @ColumnInfo("Tag_isMusicBrainzGenre") val isMusicBrainzGenre: Boolean = false,
) : Parcelable

fun Iterable<Tag>.toAlbumTags(albumId: String) = map { AlbumTag(albumId, it.name) }
