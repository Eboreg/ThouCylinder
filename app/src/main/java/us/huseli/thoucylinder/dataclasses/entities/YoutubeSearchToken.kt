package us.huseli.thoucylinder.dataclasses.entities

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
@Immutable
data class YoutubeSearchToken(
    @ColumnInfo("YoutubeSearchToken_query") @PrimaryKey val query: String,
    @ColumnInfo("YoutubeSearchToken_prevKey") val prevKey: String?,
    @ColumnInfo("YoutubeSearchToken_nextKey") val nextKey: String?,
)
