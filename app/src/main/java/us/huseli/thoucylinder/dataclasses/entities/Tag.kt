package us.huseli.thoucylinder.dataclasses.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity
data class Tag(
    @ColumnInfo("Tag_name") @PrimaryKey val name: String,
    @ColumnInfo("Tag_isMusicBrainzGenre") val isMusicBrainzGenre: Boolean = false,
) : Parcelable