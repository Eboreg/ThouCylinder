package us.huseli.thoucylinder.dataclasses.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class YoutubeSearchToken(
    @PrimaryKey val query: String,
    val prevKey: String?,
    val nextKey: String?,
)

@Entity(
    primaryKeys = ["query", "trackId"],
    indices = [Index("trackId")],
)
data class YoutubeQueryTrack(
    val query: String,
    val trackId: UUID,
    val position: Int,
)
