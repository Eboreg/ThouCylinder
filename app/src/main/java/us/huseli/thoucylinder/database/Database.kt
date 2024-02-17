package us.huseli.thoucylinder.database

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.AlbumTag
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.PlaylistTrack
import us.huseli.thoucylinder.dataclasses.entities.QueueTrack
import us.huseli.thoucylinder.dataclasses.entities.Tag
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.entities.YoutubeQueryTrack
import us.huseli.thoucylinder.dataclasses.entities.YoutubeSearchToken
import java.util.concurrent.Executors

@androidx.room.Database(
    entities = [
        Tag::class,
        Track::class,
        Album::class,
        Playlist::class,
        PlaylistTrack::class,
        YoutubeSearchToken::class,
        YoutubeQueryTrack::class,
        QueueTrack::class,
        AlbumTag::class,
    ],
    exportSchema = false,
    version = 83,
)
@TypeConverters(Converters::class)
abstract class Database : RoomDatabase() {
    abstract fun artistDao(): ArtistDao
    abstract fun trackDao(): TrackDao
    abstract fun albumDao(): AlbumDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun youtubeSearchDao(): YoutubeSearchDao
    abstract fun queueDao(): QueueDao

    companion object {
        fun build(context: Context): Database {
            val builder = Room
                .databaseBuilder(context.applicationContext, Database::class.java, "db.sqlite3")
                .fallbackToDestructiveMigration()

            if (BuildConfig.DEBUG) {
                class Callback : QueryCallback {
                    override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
                        if (
                            !sqlQuery.startsWith("BEGIN DEFERRED TRANSACTION") &&
                            !sqlQuery.startsWith("TRANSACTION SUCCESSFUL") &&
                            !sqlQuery.startsWith("END TRANSACTION") &&
                            !sqlQuery.contains("room_table_modification_log") &&
                            !sqlQuery.startsWith("DROP TRIGGER IF EXISTS")
                        ) Log.i("Database", "$sqlQuery\nbindArgs=$bindArgs")
                    }
                }

                val executor = Executors.newSingleThreadExecutor()
                builder.setQueryCallback(Callback(), executor)
            }

            return builder.build()
        }
    }
}
