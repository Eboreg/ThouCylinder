package us.huseli.thoucylinder.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.database.dao.AlbumDao
import us.huseli.thoucylinder.database.dao.TrackDao
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): Database = Database.build(context)

    @Provides
    fun provideTrackDao(database: Database): TrackDao = database.trackDao()

    @Provides
    fun provideAlbumDao(database: Database): AlbumDao = database.albumDao()
}
