package us.huseli.thoucylinder.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import us.huseli.thoucylinder.data.dao.AlbumDao
import us.huseli.thoucylinder.data.dao.TrackDao
import us.huseli.thoucylinder.data.entities.Album
import us.huseli.thoucylinder.data.entities.Track

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
