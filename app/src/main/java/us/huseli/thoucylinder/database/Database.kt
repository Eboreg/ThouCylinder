package us.huseli.thoucylinder.database

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.dataclasses.Album
import us.huseli.thoucylinder.dataclasses.AlbumGenre
import us.huseli.thoucylinder.dataclasses.AlbumStyle
import us.huseli.thoucylinder.dataclasses.Genre
import us.huseli.thoucylinder.dataclasses.Playlist
import us.huseli.thoucylinder.dataclasses.PlaylistTrack
import us.huseli.thoucylinder.dataclasses.Style
import us.huseli.thoucylinder.dataclasses.Track
import java.util.concurrent.Executors

@androidx.room.Database(
    entities = [
        Track::class,
        Album::class,
        Genre::class,
        Style::class,
        AlbumGenre::class,
        AlbumStyle::class,
        Playlist::class,
        PlaylistTrack::class,
    ],
    exportSchema = false,
    version = 29,
)
@TypeConverters(Converters::class)
abstract class Database : RoomDatabase() {
    abstract fun musicDao(): MusicDao

    companion object {
        fun build(context: Context): Database {
            val builder = Room
                .databaseBuilder(context.applicationContext, Database::class.java, "db.sqlite3")
                .fallbackToDestructiveMigration()

            if (BuildConfig.DEBUG) {
                class Callback : QueryCallback {
                    override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
                        Log.i("Database", "$sqlQuery, bindArgs=$bindArgs")
                    }
                }

                val executor = Executors.newSingleThreadExecutor()
                builder.setQueryCallback(Callback(), executor)
            }

            return builder.build()
        }
    }
}
