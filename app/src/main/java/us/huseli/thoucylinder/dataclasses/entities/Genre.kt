package us.huseli.thoucylinder.dataclasses.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Genre(
    @ColumnInfo("Genre_genreName") @PrimaryKey val genreName: String,
)
