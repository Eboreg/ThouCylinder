package us.huseli.thoucylinder.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import us.huseli.thoucylinder.database.AlbumDao
import us.huseli.thoucylinder.database.ArtistDao
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.database.PlaylistDao
import us.huseli.thoucylinder.database.QueueDao
import us.huseli.thoucylinder.database.TrackDao
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): Database = Database.build(context)

    @Provides
    fun provideArtistDao(database: Database): ArtistDao = database.artistDao()

    @Provides
    fun provideTrackDao(database: Database): TrackDao = database.trackDao()

    @Provides
    fun provideAlbumDao(database: Database): AlbumDao = database.albumDao()

    @Provides
    fun providePlaylistDao(database: Database): PlaylistDao = database.playlistDao()

    @Provides
    fun provideQueueDao(database: Database): QueueDao = database.queueDao()
}
