package us.huseli.thoucylinder.data

import androidx.room.TypeConverter
import java.io.File
import java.util.UUID

object Converters {
    @TypeConverter
    @JvmStatic
    fun uuidToString(value: UUID?): String? = value?.toString()

    @TypeConverter
    @JvmStatic
    fun stringToUuid(value: String?): UUID? = value?.let { UUID.fromString(it) }

    @TypeConverter
    @JvmStatic
    fun fileToString(value: File?): String? = value?.toString()

    @TypeConverter
    @JvmStatic
    fun stringToFile(value: String?): File? = value?.let { File(it) }
}
