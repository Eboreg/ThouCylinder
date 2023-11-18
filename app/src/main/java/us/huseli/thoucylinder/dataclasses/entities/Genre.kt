package us.huseli.thoucylinder.dataclasses.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
data class Genre(
    @ColumnInfo("Genre_genreName") @PrimaryKey val genreName: String,
) : Parcelable
