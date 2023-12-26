package us.huseli.thoucylinder.repositories

import us.huseli.thoucylinder.database.Database
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtistRepository @Inject constructor(database: Database) {
    private val artistDao = database.artistDao()

    val trackArtistPojos = artistDao.flowTrackArtistPojos()
    val albumArtistPojos = artistDao.flowAlbumArtistPojos()
}
