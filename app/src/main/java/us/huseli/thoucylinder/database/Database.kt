package us.huseli.thoucylinder.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import us.huseli.thoucylinder.database.dao.AlbumDao
import us.huseli.thoucylinder.database.dao.TrackDao
import us.huseli.thoucylinder.dataclasses.Album
import us.huseli.thoucylinder.dataclasses.Track

@androidx.room.Database(
    entities = [Track::class, Album::class],
    exportSchema = false,
    version = 4,
)
@TypeConverters(Converters::class)
abstract class Database : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun albumDao(): AlbumDao

    companion object {
        fun build(context: Context): Database {
            return Room
                .databaseBuilder(context.applicationContext, Database::class.java, "db.sqlite3")
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
