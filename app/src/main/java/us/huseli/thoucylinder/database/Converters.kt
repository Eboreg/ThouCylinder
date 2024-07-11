package us.huseli.thoucylinder.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

object Converters {
    private val gson: Gson = GsonBuilder().create()
    private val stringListType = object : TypeToken<List<String>>() {}

    @TypeConverter
    @JvmStatic
    fun stringListToString(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    @JvmStatic
    fun stringToStringList(value: String): List<String> =
        gson.fromJson(value.trim('[', ']').let { "[$it]" }, stringListType)
}
