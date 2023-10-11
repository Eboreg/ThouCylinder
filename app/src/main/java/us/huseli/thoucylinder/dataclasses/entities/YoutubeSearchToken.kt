package us.huseli.thoucylinder.dataclasses.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class YoutubeSearchToken(
    @ColumnInfo("YoutubeSearchToken_query") @PrimaryKey val query: String,
    @ColumnInfo("YoutubeSearchToken_prevKey") val prevKey: String?,
    @ColumnInfo("YoutubeSearchToken_nextKey") val nextKey: String?,
)
