package us.huseli.thoucylinder.database

import android.net.Uri
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.time.Instant
import java.util.UUID

object Converters {
    private val gson: Gson = GsonBuilder().create()
    private val listType = object : TypeToken<List<String>>() {}

    @TypeConverter
    @JvmStatic
    fun stringListToString(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    @JvmStatic
    fun stringToStringList(value: String): List<String> = gson.fromJson(value, listType)

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

    @TypeConverter
    @JvmStatic
    fun uriToString(value: Uri?): String? = value?.toString()

    @TypeConverter
    @JvmStatic
    fun stringToUri(value: String?): Uri? = value?.let { Uri.parse(it) }

    @TypeConverter
    @JvmStatic
    fun instantToLong(value: Instant): Long = value.epochSecond

    @TypeConverter
    @JvmStatic
    fun longToInstant(value: Long): Instant = Instant.ofEpochSecond(value)
}
