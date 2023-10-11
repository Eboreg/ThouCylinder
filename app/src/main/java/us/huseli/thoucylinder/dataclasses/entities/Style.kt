package us.huseli.thoucylinder.dataclasses.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Style(
    @ColumnInfo("Style_styleName") @PrimaryKey val styleName: String,
)
