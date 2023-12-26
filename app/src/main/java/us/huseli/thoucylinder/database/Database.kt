package us.huseli.thoucylinder.database

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.AlbumGenre
import us.huseli.thoucylinder.dataclasses.entities.AlbumStyle
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.dataclasses.entities.LastFmAlbum
import us.huseli.thoucylinder.dataclasses.entities.LastFmTrack
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.PlaylistTrack
import us.huseli.thoucylinder.dataclasses.entities.QueueTrack
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbum
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbumArtist
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbumGenre
import us.huseli.thoucylinder.dataclasses.entities.SpotifyArtist
import us.huseli.thoucylinder.dataclasses.entities.SpotifyTrack
import us.huseli.thoucylinder.dataclasses.entities.SpotifyTrackArtist
import us.huseli.thoucylinder.dataclasses.entities.Style
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.entities.YoutubeQueryTrack
import us.huseli.thoucylinder.dataclasses.entities.YoutubeSearchToken
import java.util.concurrent.Executors

@androidx.room.Database(
    entities = [
        Genre::class,
        Style::class,
        Track::class,
        Album::class,
        Playlist::class,
        PlaylistTrack::class,
        YoutubeSearchToken::class,
        YoutubeQueryTrack::class,
        QueueTrack::class,
        AlbumGenre::class,
        AlbumStyle::class,
        SpotifyAlbum::class,
        SpotifyAlbumArtist::class,
        SpotifyAlbumGenre::class,
        SpotifyArtist::class,
        SpotifyTrack::class,
        SpotifyTrackArtist::class,
        LastFmAlbum::class,
        LastFmTrack::class,
    ],
    exportSchema = false,
    version = 73,
)
@TypeConverters(Converters::class)
abstract class Database : RoomDatabase() {
    abstract fun artistDao(): ArtistDao
    abstract fun trackDao(): TrackDao
    abstract fun albumDao(): AlbumDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun youtubeSearchDao(): YoutubeSearchDao
    abstract fun queueDao(): QueueDao
    abstract fun spotifyDao(): SpotifyDao
    abstract fun lastFmDao(): LastFmDao

    companion object {
        fun build(context: Context): Database {
            val builder = Room
                .databaseBuilder(context.applicationContext, Database::class.java, "db.sqlite3")
                .fallbackToDestructiveMigration()

            if (BuildConfig.DEBUG) {
                class Callback : QueryCallback {
                    override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
                        Log.i("Database", "$sqlQuery\nbindArgs=$bindArgs")
                    }
                }

                val executor = Executors.newSingleThreadExecutor()
                builder.setQueryCallback(Callback(), executor)
            }

            return builder.build()
        }
    }
}
