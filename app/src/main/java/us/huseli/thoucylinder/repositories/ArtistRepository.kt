package us.huseli.thoucylinder.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import org.apache.commons.text.similarity.LevenshteinDistance
import us.huseli.thoucylinder.AbstractScopeHolder
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.album.IAlbum
import us.huseli.thoucylinder.dataclasses.album.IAlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.artist.AlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.Artist
import us.huseli.thoucylinder.dataclasses.artist.ArtistCombo
import us.huseli.thoucylinder.dataclasses.artist.ArtistWithCounts
import us.huseli.thoucylinder.dataclasses.artist.IAlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.IArtist
import us.huseli.thoucylinder.dataclasses.artist.ITrackArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.TrackArtistCredit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtistRepository @Inject constructor(database: Database) : AbstractScopeHolder() {
    private val artistDao = database.artistDao()
    private val levenshtein = LevenshteinDistance()

    private val allArtists: Flow<List<Artist>> = artistDao.flowArtists().distinctUntilChanged()

    val artistsWithTracksOrAlbums: Flow<List<Artist>> = artistDao.flowArtistsWithTracksOrAlbums().distinctUntilChanged()
    val artistCombos: Flow<List<ArtistCombo>> = artistDao.flowArtistCombos()

    suspend fun clearArtists() = onIOThread { artistDao.clearArtists() }

    suspend fun deleteOrphanArtists() = artistDao.deleteOrphanArtists()

    fun flowArtistComboById(artistId: String): Flow<ArtistCombo?> = artistDao.flowArtistComboById(artistId)

    fun flowArtistsByAlbumId(albumId: String): Flow<List<ArtistWithCounts>> = artistDao.flowArtistsByAlbumId(albumId)

    suspend fun getArtist(id: String) = artistDao.getArtist(id)

    suspend fun getArtistNameSuggestions(name: String, limit: Int = 10) = allArtists.first()
        .filter { it.name.contains(name, true) }
        .map { it.name }
        .sortedBy { levenshtein.apply(name.lowercase(), it.lowercase()) }
        .take(limit)

    suspend fun insertAlbumArtists(albumArtists: Collection<IAlbumArtistCredit>): List<AlbumArtistCredit> {
        return if (albumArtists.isNotEmpty()) onIOThread { artistDao.insertAlbumArtists(albumArtists) }
        else emptyList<AlbumArtistCredit>()
    }

    suspend fun insertTrackArtists(trackArtists: Collection<ITrackArtistCredit>): List<TrackArtistCredit> {
        return if (trackArtists.isNotEmpty()) onIOThread { artistDao.insertTrackArtists(trackArtists) }
        else emptyList<TrackArtistCredit>()
    }

    suspend fun listArtistsByTrackId(trackId: String): List<Artist> =
        onIOThread { artistDao.listArtistsByTrackId(trackId) }

    suspend fun setAlbumArtists(
        albumId: String,
        albumArtists: Collection<IAlbumArtistCredit>,
    ): List<AlbumArtistCredit> = onIOThread { artistDao.setAlbumArtists(albumId, albumArtists) }

    suspend fun setAlbumComboArtists(combo: IAlbumWithTracksCombo<IAlbum>) = onIOThread {
        artistDao.setAlbumArtists(combo.album.albumId, combo.artists)
        artistDao.clearTrackArtists(*combo.trackIds.toTypedArray())
        if (combo.trackCombos.isNotEmpty()) artistDao.insertTrackArtists(combo.trackCombos.flatMap { it.trackArtists })
    }

    suspend fun setArtistMusicBrainzId(artistId: String, musicBrainzId: String) =
        onIOThread { artistDao.setMusicBrainzId(artistId, musicBrainzId) }

    suspend fun setArtistSpotifyData(artistId: String, spotifyId: String, image: MediaStoreImage?) = onIOThread {
        artistDao.setSpotifyData(
            artistId = artistId,
            spotifyId = spotifyId,
            imageUri = image?.fullUriString,
            imageThumbnailUri = image?.thumbnailUriString,
        )
    }

    suspend fun setTrackArtists(trackId: String, trackArtists: Collection<ITrackArtistCredit>) =
        onIOThread { artistDao.setTrackArtists(trackId, trackArtists) }

    suspend fun upsertArtist(artist: IArtist) = onIOThread { artistDao.upsertArtist(artist) }
}
