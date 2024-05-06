package us.huseli.thoucylinder.repositories

import kotlinx.coroutines.flow.distinctUntilChanged
import org.apache.commons.text.similarity.LevenshteinDistance
import us.huseli.thoucylinder.AbstractScopeHolder
import us.huseli.thoucylinder.MutexCache
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.UnsavedArtist
import us.huseli.thoucylinder.dataclasses.entities.AlbumArtist
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.entities.TrackArtist
import us.huseli.thoucylinder.dataclasses.pojos.TopLocalSpotifyArtistPojo
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyArtist
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtistRepository @Inject constructor(database: Database) : AbstractScopeHolder() {
    inner class ArtistCache : MutexCache<UnsavedArtist, String, Artist>(
        fetchMethod = { artistDao.createOrUpdateArtist(it) },
        itemToKey = { it.name },
        debugLabel = "artistCache",
    ) {
        suspend fun getByName(name: String): Artist = get(UnsavedArtist(name = name))
    }

    private val artistDao = database.artistDao()
    private val levenshtein = LevenshteinDistance()

    private val allArtists = artistDao.flowArtists().distinctUntilChanged().stateEagerly(emptyList())

    val artistsWithTracksOrAlbums = artistDao.flowArtistsWithTracksOrAlbums().distinctUntilChanged()
    val artistCombos = artistDao.flowArtistCombos()
    val artistCache = ArtistCache()

    suspend fun clearArtists() = artistDao.clearArtists()

    suspend fun clearTrackArtists(trackIds: Collection<String>) = artistDao.clearTrackArtists(*trackIds.toTypedArray())

    fun flowArtistById(id: String) = artistDao.flowArtistById(id)

    suspend fun getArtist(id: String) = artistDao.getArtist(id)

    fun getArtistNameSuggestions(name: String, limit: Int = 10) = allArtists.value
        .filter { it.name.contains(name, true) }
        .map { it.name }
        .sortedBy { levenshtein.apply(name.lowercase(), it.lowercase()) }
        .take(limit)

    suspend fun insertAlbumArtists(albumArtists: Collection<AlbumArtist>) {
        if (albumArtists.isNotEmpty()) artistDao.insertAlbumArtists(albumArtists)
    }

    suspend fun insertTrackArtists(trackArtists: Collection<TrackArtist>) {
        if (trackArtists.isNotEmpty()) artistDao.insertTrackArtists(trackArtists)
    }

    suspend fun listTopSpotifyArtists(limit: Int = 10): List<TopLocalSpotifyArtistPojo> =
        artistDao.listTopSpotifyArtists(limit)

    suspend fun listTrackArtistCredits(trackId: String): List<TrackArtistCredit> =
        artistDao.listTrackArtistCredits(trackId)

    suspend fun setAlbumArtists(albumId: String, albumArtists: Collection<AlbumArtist>) =
        artistDao.setAlbumArtists(albumId, albumArtists)

    suspend fun setArtistMusicBrainzId(artistId: String, musicBrainzId: String) =
        artistDao.setMusicBrainzId(artistId, musicBrainzId)

    suspend fun setArtistSpotifyData(artistId: String, spotifyId: String, image: MediaStoreImage?) =
        artistDao.setSpotifyData(
            artistId = artistId,
            spotifyId = spotifyId,
            imageUri = image?.fullUriString,
            imageThumbnailUri = image?.thumbnailUriString,
            imageHash = image?.hash
        )

    suspend fun setArtistSpotifyId(artistId: String, spotifyId: String) = artistDao.setSpotifyId(artistId, spotifyId)

    suspend fun setTrackArtists(trackId: String, trackArtists: Collection<TrackArtist>) =
        artistDao.setTrackArtists(trackId, trackArtists)

    suspend fun upsertSpotifyArtist(artist: SpotifyArtist) = artistDao.upsertSpotifyArtist(artist)
}
