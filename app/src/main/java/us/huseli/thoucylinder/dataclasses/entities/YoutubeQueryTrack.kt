package us.huseli.thoucylinder.dataclasses.entities

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    primaryKeys = ["YoutubeQueryTrack_query", "YoutubeQueryTrack_trackId"],
    indices = [Index("YoutubeQueryTrack_trackId")],
)
@Immutable
data class YoutubeQueryTrack(
    @ColumnInfo("YoutubeQueryTrack_query") val query: String,
    @ColumnInfo("YoutubeQueryTrack_trackId") val trackId: String,
    @ColumnInfo("YoutubeQueryTrack_position") val position: Int,
)
