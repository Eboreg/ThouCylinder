package us.huseli.thoucylinder.database

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.dataclasses.combos.AlbumCombo
import us.huseli.thoucylinder.dataclasses.combos.TrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.AlbumArtist
import us.huseli.thoucylinder.dataclasses.entities.AlbumTag
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.PlaylistTrack
import us.huseli.thoucylinder.dataclasses.entities.QueueTrack
import us.huseli.thoucylinder.dataclasses.entities.Radio
import us.huseli.thoucylinder.dataclasses.entities.RadioTrack
import us.huseli.thoucylinder.dataclasses.entities.Tag
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.entities.TrackArtist
import us.huseli.thoucylinder.dataclasses.entities.YoutubeQueryTrack
import us.huseli.thoucylinder.dataclasses.entities.YoutubeSearchToken
import us.huseli.thoucylinder.dataclasses.views.AlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.views.RadioView
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit
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
        Artist::class,
        AlbumArtist::class,
        TrackArtist::class,
        Radio::class,
        RadioTrack::class,
    ],
    views = [
        AlbumArtistCredit::class,
        TrackArtistCredit::class,
        TrackCombo::class,
        AlbumCombo::class,
        RadioView::class,
    ],
    exportSchema = false,
    version = 95,
)
@TypeConverters(Converters::class)
abstract class Database : RoomDatabase() {
    abstract fun artistDao(): ArtistDao
    abstract fun trackDao(): TrackDao
    abstract fun albumDao(): AlbumDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun youtubeSearchDao(): YoutubeSearchDao
    abstract fun queueDao(): QueueDao
    abstract fun radioDao(): RadioDao

    companion object {
        fun build(context: Context): Database {
            val builder = Room
                .databaseBuilder(context.applicationContext, Database::class.java, "db.sqlite3")
                .fallbackToDestructiveMigration()

            if (BuildConfig.DEBUG) {
                class DBQueryCallback : QueryCallback {
                    override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
                        if (
                            !sqlQuery.startsWith("BEGIN DEFERRED TRANSACTION")
                            && !sqlQuery.startsWith("TRANSACTION SUCCESSFUL")
                            && !sqlQuery.startsWith("END TRANSACTION")
                            && !sqlQuery.contains("room_table_modification_log")
                            && !sqlQuery.startsWith("DROP TRIGGER IF EXISTS")
                        ) {
                            var index = 0

                            Log.i("Database", sqlQuery.replace(Regex("\\?")) { "'${bindArgs.getOrNull(index++)}'" })
                        }
                    }
                }

                val executor = Executors.newSingleThreadExecutor()
                builder.setQueryCallback(DBQueryCallback(), executor)
            }

            return builder.build()
        }
    }
}
