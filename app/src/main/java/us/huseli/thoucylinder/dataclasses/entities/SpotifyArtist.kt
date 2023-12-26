package us.huseli.thoucylinder.dataclasses.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SpotifyArtist(
    @ColumnInfo("SpotifyArtist_href") val href: String,
    @ColumnInfo("SpotifyArtist_id") @PrimaryKey val id: String,
    @ColumnInfo("SpotifyArtist_name") val name: String,
    @ColumnInfo("SpotifyArtist_uri") val uri: String,
)
