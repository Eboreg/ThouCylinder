package us.huseli.thoucylinder.dataclasses.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    primaryKeys = ["TrackArtist_trackId", "TrackArtist_artistId"],
    indices = [Index("TrackArtist_artistId")],
    foreignKeys = [
        ForeignKey(
            entity = Track::class,
            parentColumns = ["Track_trackId"],
            childColumns = ["TrackArtist_trackId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Artist::class,
            parentColumns = ["Artist_id"],
            childColumns = ["TrackArtist_artistId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
)
data class TrackArtist(
    @ColumnInfo("TrackArtist_trackId") val trackId: String,
    @ColumnInfo("TrackArtist_artistId") val artistId: String,
    @ColumnInfo("TrackArtist_joinPhrase") val joinPhrase: String = "/",
    @ColumnInfo("TrackArtist_position") val position: Int = 0,
) : Comparable<TrackArtist> {
    override fun compareTo(other: TrackArtist): Int = position - other.position
}
